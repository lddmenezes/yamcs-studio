package org.yamcs.studio.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

import org.yamcs.api.YamcsConnectionProperties;
import org.yamcs.api.ws.WebSocketClientCallback;
import org.yamcs.api.ws.WebSocketRequest;
import org.yamcs.protobuf.Rest.CreateProcessorRequest;
import org.yamcs.protobuf.Rest.EditClientRequest;
import org.yamcs.protobuf.Rest.EditProcessorRequest;
import org.yamcs.protobuf.Web.ConnectionInfo;
import org.yamcs.protobuf.Web.WebSocketServerMessage.WebSocketSubscriptionData;
import org.yamcs.protobuf.YamcsManagement.ClientInfo;
import org.yamcs.protobuf.YamcsManagement.ClientInfo.ClientState;
import org.yamcs.protobuf.YamcsManagement.ProcessorInfo;
import org.yamcs.protobuf.YamcsManagement.ServiceState;
import org.yamcs.protobuf.YamcsManagement.Statistics;
import org.yamcs.protobuf.YamcsManagement.YamcsInstance;
import org.yamcs.studio.core.YamcsPlugin;
import org.yamcs.studio.core.client.YamcsClient;

/**
 * Provides access to aggregated state on yamcs management-type information.
 * <p>
 * There should be only one long-lived instance of this class, which goes down together with the application (same
 * lifecycle as {@link YamcsPlugin}). This catalogue deals with maintaining correct state accross connection-reconnects,
 * so listeners only need to register once.
 */
public class ManagementCatalogue implements Catalogue, WebSocketClientCallback {

    private static final Logger log = Logger.getLogger(ManagementCatalogue.class.getName());

    private Set<ManagementListener> managementListeners = new CopyOnWriteArraySet<>();
    private Set<InstanceListener> instanceListeners = new CopyOnWriteArraySet<>();

    // instance -> processorName -> info
    private Map<String, Map<String, ProcessorInfo>> processorInfoByInstance = new ConcurrentHashMap<>();
    private Map<Integer, ClientInfo> clientInfoById = new ConcurrentHashMap<>();

    // Redundant, but quickly accessible
    private int currentClientId = -1;

    public static ManagementCatalogue getInstance() {
        return YamcsPlugin.getDefault().getCatalogue(ManagementCatalogue.class);
    }

    @Override
    public void onYamcsConnected() {
        YamcsClient yamcsClient = YamcsPlugin.getYamcsClient();
        yamcsClient.subscribe(new WebSocketRequest("management", "subscribe"), this);
    }

    @Override
    public void onMessage(WebSocketSubscriptionData msg) {
        if (msg.hasConnectionInfo()) {
            ConnectionInfo connectionInfo = msg.getConnectionInfo();
            YamcsInstance instance = connectionInfo.getInstance();
            log.fine("Instance " + instance.getName() + ": " + instance.getState());
            managementListeners.forEach(l -> l.instanceUpdated(connectionInfo));
        }

        if (msg.hasClientInfo()) {
            ClientInfo clientInfo = msg.getClientInfo();
            if (clientInfo.getState() == ClientState.DISCONNECTED) {
                ClientInfo previousClientInfo = clientInfoById.remove(clientInfo.getId());
                checkOwnClientState(previousClientInfo, clientInfo);

                managementListeners.forEach(l -> l.clientDisconnected(clientInfo));
            } else {
                ClientInfo previousClientInfo = clientInfoById.put(clientInfo.getId(), clientInfo);
                checkOwnClientState(previousClientInfo, clientInfo);

                managementListeners.forEach(l -> l.clientUpdated(clientInfo));
            }
        }

        if (msg.hasProcessorInfo()) {
            ProcessorInfo processorInfo = msg.getProcessorInfo();
            String instance = processorInfo.getInstance();
            Map<String, ProcessorInfo> instanceProcessors = processorInfoByInstance.get(instance);
            if (instanceProcessors == null) {
                instanceProcessors = new ConcurrentHashMap<>();
                processorInfoByInstance.put(instance, instanceProcessors);
            }
            if (processorInfo.getState() == ServiceState.TERMINATED) {
                instanceProcessors.remove(processorInfo.getName());
            } else {
                instanceProcessors.put(processorInfo.getName(), processorInfo);
            }

            managementListeners.forEach(l -> l.processorUpdated(processorInfo));
        }

        if (msg.hasStatistics()) {
            Statistics statistics = msg.getStatistics();
            managementListeners.forEach(l -> l.statisticsUpdated(statistics));
        }
    }

    @Override
    public void instanceChanged(String oldInstance, String newInstance) {
        // Ignore. It's this catalogues responsability to inform other InstanceListeners;
        // Further, the information in this catalogue is server-wide. Not instance-specific.
    }

    @Override
    public void onYamcsDisconnected() {
        // Clear everything, we'll get a fresh set upon connect
        clientInfoById.clear();
        processorInfoByInstance.clear();
        currentClientId = -1;

        managementListeners.forEach(ManagementListener::clearAllManagementData);
    }

    public void addManagementListener(ManagementListener listener) {
        managementListeners.add(listener);

        // Inform listeners of the current model
        processorInfoByInstance.forEach((k, m) -> {
            m.forEach((sk, v) -> listener.processorUpdated(v));
        });
        clientInfoById.forEach((k, v) -> listener.clientUpdated(v));
    }

    public void removeManagementListener(ManagementListener listener) {
        managementListeners.remove(listener);
    }

    public void addInstanceListener(InstanceListener listener) {
        instanceListeners.add(listener);
    }

    public void removeInstanceListener(InstanceListener listener) {
        instanceListeners.remove(listener);
    }

    private void checkOwnClientState(ClientInfo old, ClientInfo incoming) {
        if (incoming.getCurrentClient()) {
            if (incoming.getState() == ClientState.DISCONNECTED) {
                currentClientId = -1;
            } else {
                currentClientId = incoming.getId();
                if (old != null) {
                    String oldInstance = old.getInstance();
                    String newInstance = incoming.getInstance();
                    if (!oldInstance.equals(newInstance)) {
                        log.info(String.format("Client instance changed from '%s' "
                                + "to '%s'. Notifying listeners.", oldInstance, newInstance));
                        instanceListeners.forEach(l -> l.instanceChanged(oldInstance, newInstance));
                    }
                }
            }
        }
    }

    /**
     * Returns processor with matching name for the currently connected instance
     */
    public ProcessorInfo getProcessorInfo(String processorName) {
        String instance = getCurrentYamcsInstance();
        return getProcessorInfo(instance, processorName);
    }

    public ProcessorInfo getProcessorInfo(String yamcsInstance, String processorName) {
        Map<String, ProcessorInfo> instanceProcessors = processorInfoByInstance.get(yamcsInstance);
        if (instanceProcessors != null) {
            return instanceProcessors.get(processorName);
        }
        return null;
    }

    public ClientInfo getClientInfo(int clientId) {
        return clientInfoById.get(clientId);
    }

    public ClientInfo getCurrentClientInfo() {
        return clientInfoById.get(currentClientId);
    }

    // Careful we must support the case where Yamcs itself changes the instance of our client
    // TODO maybe remove this and instead just store an instance field in YamcsClient ?
    public static String getCurrentYamcsInstance() {
        ManagementCatalogue catalogue = getInstance();
        if (catalogue == null) {
            return null;
        }
        ClientInfo ci = catalogue.getCurrentClientInfo();
        if (ci != null) {
            return ci.getInstance();
        } else {
            // Fallback (initial connection properties
            YamcsClient yamcsClient = YamcsPlugin.getYamcsClient();
            YamcsConnectionProperties props = yamcsClient.getYamcsConnectionProperties();
            return (props != null) ? props.getInstance() : null;
        }
    }

    public ProcessorInfo getCurrentProcessorInfo() {
        ClientInfo ci = clientInfoById.get(currentClientId);
        return (ci != null) ? getProcessorInfo(ci.getInstance(), ci.getProcessorName()) : null;
    }

    /**
     * Returns processors for any instance
     */
    public List<ProcessorInfo> getProcessors() {
        List<ProcessorInfo> result = new ArrayList<>();
        processorInfoByInstance.forEach((k, m) -> {
            result.addAll(m.values());
        });
        return result;
    }

    /**
     * Returns processors for the specified instance
     */
    public List<ProcessorInfo> getProcessors(String yamcsInstance) {
        Map<String, ProcessorInfo> instanceProcessors = processorInfoByInstance.get(yamcsInstance);
        if (instanceProcessors != null) {
            return new ArrayList<>(instanceProcessors.values());
        } else {
            return Collections.emptyList();
        }
    }

    public List<ClientInfo> getClients() {
        return new ArrayList<>(clientInfoById.values());
    }

    public CompletableFuture<byte[]> createProcessorRequest(String yamcsInstance, CreateProcessorRequest request) {
        YamcsClient yamcsClient = YamcsPlugin.getYamcsClient();
        return yamcsClient.post("/processors/" + yamcsInstance, request);
    }

    public CompletableFuture<byte[]> editProcessorRequest(String yamcsInstance, String processor,
            EditProcessorRequest request) {
        YamcsClient yamcsClient = YamcsPlugin.getYamcsClient();
        return yamcsClient.patch("/processors/" + yamcsInstance + "/" + processor, request);
    }

    public CompletableFuture<byte[]> editClientRequest(int clientId, EditClientRequest request) {
        YamcsClient yamcsClient = YamcsPlugin.getYamcsClient();
        return yamcsClient.patch("/clients/" + clientId, request);
    }

    public CompletableFuture<byte[]> fetchInstanceInformationRequest(String yamcsInstance) {
        YamcsClient yamcsClient = YamcsPlugin.getYamcsClient();
        return yamcsClient.get("/instances/" + yamcsInstance + "?aggregate", null);
    }

    public CompletableFuture<byte[]> restartInstance(String yamcsInstance) {
        YamcsClient yamcsClient = YamcsPlugin.getYamcsClient();
        return yamcsClient.post("/instances/" + yamcsInstance + "?state=restarted", null);
    }
}
