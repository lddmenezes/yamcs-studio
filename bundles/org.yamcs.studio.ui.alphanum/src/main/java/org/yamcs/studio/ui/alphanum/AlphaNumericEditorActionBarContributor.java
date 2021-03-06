package org.yamcs.studio.ui.alphanum;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.yamcs.studio.ui.alphanum.actions.AddNewParameterAction;
import org.yamcs.studio.ui.alphanum.actions.ClearAction;
import org.yamcs.studio.ui.alphanum.actions.RemoveAction;


public class AlphaNumericEditorActionBarContributor extends EditorActionBarContributor {
    private IToolBarManager manager;
    final private AddNewParameterAction add = new AddNewParameterAction(null);
    final private RemoveAction remove = new RemoveAction(null);
   // final private RestoreAction restore = new RestoreAction(null);
    final private ClearAction clearAll = new ClearAction(null);



    @Override
    public void contributeToToolBar(final IToolBarManager mgr) {
    	mgr.add(add);
        mgr.add(remove);
       // mgr.add(restore);
        mgr.add(clearAll);
        mgr.add(new Separator());
        this.manager = mgr;
    }

    @Override
    public void setActiveEditor(final IEditorPart target) {
        final AlphaNumericEditor editor = (AlphaNumericEditor) target;
        add.setViewer(editor.getParameterTable());
        remove.setViewer(editor.getParameterTable());
      //  restore.setViewer(editor.getParameterTable());
        clearAll.setViewer(editor.getParameterTable());
        manager.update(true);

    }


    @Override
    public void dispose() {
        add.setViewer(null);
        remove.setViewer(null);
     //   restore.setViewer(null);
        clearAll.setViewer(null);
        super.dispose();
    }
}
