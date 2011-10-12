package org.ratson.pentagrid.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.GridBagLayout;
import javax.swing.BoxLayout;
import javax.swing.border.TitledBorder;
import javax.swing.Box;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JTextField;
import java.awt.Dialog.ModalExclusionType;
import java.awt.Dialog.ModalityType;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.SwingConstants;

public class SettingsDialog extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private JTextField fldExportSize;
	private boolean modalResult=false;
	
	private final ActionListener buttonActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("OK")){
				try{ 
					readValues();
					modalResult = true;
					setVisible(false);
				}catch( Exception err ){
					JOptionPane.showMessageDialog(SettingsDialog.this, err.getMessage() );
				}
			}else if(e.getActionCommand().equals("Cancel")){
				modalResult = false;
				setVisible(false);
			}else throw new RuntimeException("Bad action: "+e.getActionCommand());
		}
	};
	private Settings settings=null;
	private JTextField fldRandomAreaSize;
	private JTextField fldRandomFillPercent;
	private JCheckBox chckbxAntiAliasExport;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			SettingsDialog dialog = new SettingsDialog( new Settings() );
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**Put values from the GUI to the settings objct, validating them
	 * @throws Exception */
	protected void readValues() throws Exception {
		try{
			settings.exportImageSize = Integer.parseInt( fldExportSize.getText() );
			settings.exportAntiAlias = chckbxAntiAliasExport.isEnabled();
			settings.randomFieldRadius = Integer.parseInt( fldRandomAreaSize.getText() );
			if (settings.randomFieldRadius < 0 ) throw new Exception( "Size can not be < 0" );
			if (settings.randomFieldRadius > 20 ) throw new Exception( "Size can not be > 20" );
			settings.randomFillPercent = Math.min( 100, Math.max( 0, Integer.parseInt(fldRandomFillPercent.getText() )));
		}catch( NumberFormatException e ) {throw new Exception( "Bd number format in export image size" );}
		if ( settings.exportImageSize <=0) throw new Exception( "Image size too small" );
		if ( settings.exportImageSize >= 4096) throw new Exception( "Image size too big" );
	}
	
	protected void writeValues() {
		fldExportSize.setText( String.valueOf(settings.exportImageSize));
		chckbxAntiAliasExport.setEnabled( settings.exportAntiAlias );
		fldRandomAreaSize.setText(String.valueOf(settings.randomFieldRadius));
		fldRandomFillPercent.setText(String.valueOf( (int)(settings.randomFillPercent*100) ));
	}

	/**
	 * Create the dialog.
	 */
	public SettingsDialog( Settings s ) {
		setModalityType(ModalityType.APPLICATION_MODAL);
		setModal(true);
		setBounds(100, 100, 450, 378);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		{
			JPanel panel = new JPanel();
			panel.setBorder(new TitledBorder(null, "Field", TitledBorder.LEADING, TitledBorder.TOP, null, null));
			contentPanel.add(panel);
			GridBagLayout gbl_panel = new GridBagLayout();
			gbl_panel.columnWidths = new int[]{0, 0, 0};
			gbl_panel.rowHeights = new int[]{0, 0, 0};
			gbl_panel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
			gbl_panel.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
			panel.setLayout(gbl_panel);
			{
				JLabel lblRandomFieldSize = new JLabel("Random field size");
				GridBagConstraints gbc_lblRandomFieldSize = new GridBagConstraints();
				gbc_lblRandomFieldSize.insets = new Insets(0, 0, 5, 5);
				gbc_lblRandomFieldSize.anchor = GridBagConstraints.EAST;
				gbc_lblRandomFieldSize.gridx = 0;
				gbc_lblRandomFieldSize.gridy = 0;
				panel.add(lblRandomFieldSize, gbc_lblRandomFieldSize);
			}
			{
				fldRandomAreaSize = new JTextField();
				GridBagConstraints gbc_fldRandomAreaSize = new GridBagConstraints();
				gbc_fldRandomAreaSize.insets = new Insets(0, 0, 5, 0);
				gbc_fldRandomAreaSize.fill = GridBagConstraints.HORIZONTAL;
				gbc_fldRandomAreaSize.gridx = 1;
				gbc_fldRandomAreaSize.gridy = 0;
				panel.add(fldRandomAreaSize, gbc_fldRandomAreaSize);
				fldRandomAreaSize.setColumns(10);
			}
			{
				JLabel lblFillPercent = new JLabel("Fill percent");
				GridBagConstraints gbc_lblFillPercent = new GridBagConstraints();
				gbc_lblFillPercent.anchor = GridBagConstraints.EAST;
				gbc_lblFillPercent.insets = new Insets(0, 0, 0, 5);
				gbc_lblFillPercent.gridx = 0;
				gbc_lblFillPercent.gridy = 1;
				panel.add(lblFillPercent, gbc_lblFillPercent);
			}
			{
				fldRandomFillPercent = new JTextField();
				GridBagConstraints gbc_fldRandomFillPercent = new GridBagConstraints();
				gbc_fldRandomFillPercent.fill = GridBagConstraints.HORIZONTAL;
				gbc_fldRandomFillPercent.gridx = 1;
				gbc_fldRandomFillPercent.gridy = 1;
				panel.add(fldRandomFillPercent, gbc_fldRandomFillPercent);
				fldRandomFillPercent.setColumns(10);
			}
		}
		{
			JPanel panel = new JPanel();
			panel.setBorder(new TitledBorder(null, "Colors", TitledBorder.LEADING, TitledBorder.TOP, null, null));
			contentPanel.add(panel);
			GridBagLayout gbl_panel = new GridBagLayout();
			gbl_panel.columnWidths = new int[]{0, 0, 0};
			gbl_panel.rowHeights = new int[]{45, 0, 0, 0};
			gbl_panel.columnWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
			gbl_panel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
			panel.setLayout(gbl_panel);
			{
				JLabel lblAliveCell = new JLabel("Alive cell");
				GridBagConstraints gbc_lblAliveCell = new GridBagConstraints();
				gbc_lblAliveCell.anchor = GridBagConstraints.WEST;
				gbc_lblAliveCell.insets = new Insets(0, 0, 5, 5);
				gbc_lblAliveCell.gridx = 0;
				gbc_lblAliveCell.gridy = 0;
				panel.add(lblAliveCell, gbc_lblAliveCell);
			}
			{
				JButton button = new JButton("...");
				GridBagConstraints gbc_button = new GridBagConstraints();
				gbc_button.insets = new Insets(0, 0, 5, 0);
				gbc_button.gridx = 1;
				gbc_button.gridy = 0;
				panel.add(button, gbc_button);
			}
			{
				JLabel lblGrid = new JLabel("Grid");
				GridBagConstraints gbc_lblGrid = new GridBagConstraints();
				gbc_lblGrid.anchor = GridBagConstraints.WEST;
				gbc_lblGrid.insets = new Insets(0, 0, 5, 5);
				gbc_lblGrid.gridx = 0;
				gbc_lblGrid.gridy = 1;
				panel.add(lblGrid, gbc_lblGrid);
			}
			{
				JButton button = new JButton("...");
				GridBagConstraints gbc_button = new GridBagConstraints();
				gbc_button.insets = new Insets(0, 0, 5, 0);
				gbc_button.gridx = 1;
				gbc_button.gridy = 1;
				panel.add(button, gbc_button);
			}
			{
				JLabel lblWorldBorder = new JLabel("World border");
				GridBagConstraints gbc_lblWorldBorder = new GridBagConstraints();
				gbc_lblWorldBorder.anchor = GridBagConstraints.WEST;
				gbc_lblWorldBorder.insets = new Insets(0, 0, 0, 5);
				gbc_lblWorldBorder.gridx = 0;
				gbc_lblWorldBorder.gridy = 2;
				panel.add(lblWorldBorder, gbc_lblWorldBorder);
			}
			{
				JButton button = new JButton("...");
				GridBagConstraints gbc_button = new GridBagConstraints();
				gbc_button.gridx = 1;
				gbc_button.gridy = 2;
				panel.add(button, gbc_button);
			}
		}
		{
			JPanel panel = new JPanel();
			panel.setBorder(new TitledBorder(null, "Export", TitledBorder.LEADING, TitledBorder.TOP, null, null));
			contentPanel.add(panel);
			GridBagLayout gbl_panel = new GridBagLayout();
			gbl_panel.columnWidths = new int[]{0, 0, 0};
			gbl_panel.rowHeights = new int[]{0, 0, 0, 0};
			gbl_panel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
			gbl_panel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
			panel.setLayout(gbl_panel);
			{
				JLabel lblSize = new JLabel("Size");
				GridBagConstraints gbc_lblSize = new GridBagConstraints();
				gbc_lblSize.insets = new Insets(0, 0, 5, 5);
				gbc_lblSize.anchor = GridBagConstraints.EAST;
				gbc_lblSize.gridx = 0;
				gbc_lblSize.gridy = 0;
				panel.add(lblSize, gbc_lblSize);
			}
			{
				fldExportSize = new JTextField();
				GridBagConstraints gbc_fldExportSize = new GridBagConstraints();
				gbc_fldExportSize.insets = new Insets(0, 0, 5, 0);
				gbc_fldExportSize.fill = GridBagConstraints.HORIZONTAL;
				gbc_fldExportSize.gridx = 1;
				gbc_fldExportSize.gridy = 0;
				panel.add(fldExportSize, gbc_fldExportSize);
				fldExportSize.setColumns(10);
			}
			{
				JLabel lblBackground = new JLabel("Background");
				GridBagConstraints gbc_lblBackground = new GridBagConstraints();
				gbc_lblBackground.insets = new Insets(0, 0, 5, 5);
				gbc_lblBackground.gridx = 0;
				gbc_lblBackground.gridy = 1;
				panel.add(lblBackground, gbc_lblBackground);
			}
			{
				chckbxAntiAliasExport = new JCheckBox("Antialiasing");
				chckbxAntiAliasExport.setHorizontalAlignment(SwingConstants.TRAILING);
				GridBagConstraints gbc_chckbxAntiAliasExport = new GridBagConstraints();
				gbc_chckbxAntiAliasExport.anchor = GridBagConstraints.WEST;
				gbc_chckbxAntiAliasExport.gridwidth = 2;
				gbc_chckbxAntiAliasExport.gridx = 0;
				gbc_chckbxAntiAliasExport.gridy = 2;
				panel.add(chckbxAntiAliasExport, gbc_chckbxAntiAliasExport);
			}
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				okButton.addActionListener(buttonActionListener);
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				cancelButton.addActionListener(buttonActionListener);
				buttonPane.add(cancelButton);
			}
		}
		settings = s;
		writeValues();
	}

	public boolean showDialog(){
		setVisible(true);
		return modalResult;
	}
}
