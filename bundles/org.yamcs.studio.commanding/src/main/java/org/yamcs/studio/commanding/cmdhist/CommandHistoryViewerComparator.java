package org.yamcs.studio.commanding.cmdhist;

import static org.yamcs.studio.core.ui.utils.Comparators.INTEGER_COMPARATOR;
import static org.yamcs.studio.core.ui.utils.Comparators.LONG_COMPARATOR;
import static org.yamcs.studio.core.ui.utils.Comparators.OBJECT_COMPARATOR;
import static org.yamcs.studio.core.ui.utils.Comparators.STRING_COMPARATOR;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TableColumn;

public class CommandHistoryViewerComparator extends ViewerComparator {

    private String currentColumn;
    private boolean ascending;

    public CommandHistoryViewerComparator() {
        currentColumn = CommandHistoryView.COL_T;
        ascending = false;
    }

    public int getDirection() {
        return ascending ? SWT.UP : SWT.DOWN;
    }

    public void setColumn(TableColumn column) {
        if (column.getText().equals(currentColumn)) {
            ascending = !ascending;
        } else {
            currentColumn = column.getText();
            ascending = true;
        }
    }

    @Override
    public int compare(Viewer viewer, Object o1, Object o2) {
        CommandHistoryRecord r1 = (CommandHistoryRecord) o1;
        CommandHistoryRecord r2 = (CommandHistoryRecord) o2;
        int rc;
        switch (currentColumn) {
        case CommandHistoryView.COL_COMMAND:
            rc = STRING_COMPARATOR.compare(r1.getCommandString(), r2.getCommandString());
            break;
        case CommandHistoryView.COL_SRC_ID:
            rc = INTEGER_COMPARATOR.compare(r1.getSequenceNumber(), r2.getSequenceNumber());
            break;
        case CommandHistoryView.COL_SRC:
            rc = STRING_COMPARATOR.compare(r1.getOrigin(), r2.getOrigin());
            if (rc == 0)
                rc = STRING_COMPARATOR.compare(r1.getUsername(), r2.getUsername());
            break;
        case CommandHistoryView.COL_SEQ_ID:
            rc = STRING_COMPARATOR.compare(r1.getFinalSequenceCount(), r2.getFinalSequenceCount());
            break;
        case CommandHistoryView.COL_T:
            rc = LONG_COMPARATOR.compare(r1.getRawGenerationTime(), r2.getRawGenerationTime());
            break;
        case CommandHistoryView.COL_PTV:
            rc = OBJECT_COMPARATOR.compare(r1.getPTVInfo(), r2.getPTVInfo());
            break;
        default: // dynamic column (TODO be more clever about non-timestamp dynamic columns)
            long delta1 = r1.getAckDurationForColumn(currentColumn);
            long delta2 = r2.getAckDurationForColumn(currentColumn);
            rc = -LONG_COMPARATOR.compare(delta1, delta2);
        }

        return ascending ? rc : -rc;
    }
}
