package org.yamcs.studio.core.pvmanager;

import org.epics.pvmanager.ChannelHandler;
import org.epics.pvmanager.DataSource;
import org.yamcs.studio.core.WebSocketRegistrar;

/**
 * When running the OPIbuilder this is instantiated for every parameter separately.
 */
public class ParameterDataSource extends DataSource {

    private static WebSocketRegistrar webSocketClient;

    public ParameterDataSource() {
        super(false /* read-only */);
    }

    ParameterChannelHandler pch = null;

    @Override
    protected ChannelHandler createChannel(String channelName) {
        return new ParameterChannelHandler(channelName);
    }

}
