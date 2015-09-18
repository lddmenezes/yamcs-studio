package org.yamcs.studio.ui.archive;

import java.util.List;

import javax.swing.SwingUtilities;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.yamcs.studio.core.model.TimeCatalogue;
import org.yamcs.studio.ui.TimeInterval;

public class CreateReplayHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchPart part = HandlerUtil.getActivePartChecked(event);
        SwingUtilities.invokeLater(() -> {
            ArchiveView view = (ArchiveView) part;
            long missionTime = TimeCatalogue.getInstance().getMissionTime(true);
            missionTime -= 30 * 1000;
            TimeInterval interval = TimeInterval.starting(missionTime);
            List<String> packets = view.archivePanel.getSelectedPackets("tm");
            List<String> pps = view.archivePanel.getSelectedPackets("pp");
            Display.getDefault().asyncExec(() -> {
                CreateReplayDialog dialog = new CreateReplayDialog(Display.getCurrent().getActiveShell());
                dialog.initialize(interval, packets, pps);
                dialog.open();
            });
        });

        return null;
    }
}