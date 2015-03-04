package org.csstudio.yamcs.commanding;

import java.util.Collection;

import org.csstudio.platform.libs.yamcs.YamcsPlugin;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.yamcs.xtce.MetaCommand;

public class AddTelecommandDialog extends TitleAreaDialog {
    
    private Collection<MetaCommand> commands;

    public AddTelecommandDialog(Shell parentShell) {
        super(parentShell);
        commands = YamcsPlugin.getDefault().getCommands();
    }
    
    @Override
    public void create() {
        super.create();
        setTitle("Send a telecommand");
        //setMessage("informative message");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        GridLayout layout = new GridLayout(2, false);
        container.setLayoutData(new GridData(GridData.FILL_BOTH));
        container.setLayout(layout);

        Label lblCommand = new Label(container, SWT.NONE);
        lblCommand.setText("Template");

        Combo commandCombo = new Combo(container, SWT.BORDER | SWT.READ_ONLY);
        commandCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        for (MetaCommand command : commands) {
            if (!command.isAbstract()) {
                commandCombo.add(command.getName());
            }
        }
        
        StyledText text = new StyledText(container, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        text.setLayoutData(gd);
        
        commandCombo.addListener(SWT.Selection, event -> {
            for (MetaCommand command : commands) {
                String selected = ((Combo) event.widget).getText();
                if (!command.isAbstract() && command.getName().equals(selected)) {
                    System.out.println("yes.....");
                    
                    StringBuilder buf = new StringBuilder(command.getName());
                    if (command.getArgumentList() != null) {
                        buf.append("(\n");
                        buf.append("\targ");
                    } else {
                        buf.append("()");
                    }
                    text.setText(buf.toString());
                    break;
                }
            }
        });
        
        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        //createButton(parent, , "Validate", true);
        createButton(parent, IDialogConstants.OK_ID, "Send", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected Point getInitialSize() {
        return new Point(500, 375);
    }
}
