package org.ratson.pentagrid.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.ratson.pentagrid.Clusterizer;
import org.ratson.pentagrid.DayNightRule;
import org.ratson.pentagrid.Field;
import org.ratson.pentagrid.OrientedPath;
import org.ratson.pentagrid.Path;
import org.ratson.pentagrid.PathNavigation;
import org.ratson.pentagrid.Rule;
import org.ratson.pentagrid.RuleSyntaxException;
import org.ratson.pentagrid.TotalisticRule;
import org.ratson.pentagrid.Util;
import org.ratson.pentagrid.fields.SimpleMapField;
import org.ratson.pentagrid.gui.poincare_panel.PoincarePanelEvent;
import org.ratson.pentagrid.gui.poincare_panel.PoincarePanelListener;
import org.ratson.util.Pair;

public class MainFrame extends JFrame implements NotificationReceiver {
	
	private SimpleMapField world = new SimpleMapField();
	private TotalisticRule rule = new Rule(new int[]{3}, new int[]{2,3});
	private FarPoincarePanel panel;
	private EvaluationRunner evaluationThread=null;
	private Settings settings = new Settings();
	private JLabel lblFieldInfo, lblLocationInfo;
	private WaypointNavigator waypointNavigator=new WaypointNavigator();

	class WaypointNavigator{
		ArrayList<Path> waypoints = new ArrayList<Path>();
		int currentPosition = 0;
		public void clear(){
			waypoints.clear();
			currentPosition = 0;
		}
		public void add( Path p ){
			waypoints.add( p );
		}
		private void navigateBy( int offset ){
			if (waypoints.size()==0) return;
			currentPosition = org.ratson.util.Util.mod(currentPosition+offset, waypoints.size());
			goToCell( waypoints.get(currentPosition));
			System.out.println("Showing waypoint "+currentPosition+" of "+waypoints.size());
		}
		public void next(){ navigateBy(1); };
		public void previous(){ navigateBy(-1); }
		public void current() { navigateBy(0);	};
	}
	
	private void createUI(){
		 Box topBox = Box.createVerticalBox();
		 panel = new FarPoincarePanel( world );
		 lblFieldInfo =  new JLabel();
		 lblLocationInfo = new JLabel();
		 topBox.add( lblFieldInfo );
		 topBox.add( lblLocationInfo );
		 getContentPane().add( panel );
		 getContentPane().add( topBox, BorderLayout.NORTH );
	}
	/**Show a cell in the view*/
	public void goToCell(Path path) {
		panel.setOrigin( new OrientedPath(path, 0));
	}
	private void updateFieldInfo(){
		String infoStr = String.format("Population:%d State:%d Rule:%s", world.population(), world.getFieldState(), rule);
		lblFieldInfo.setText( infoStr );
	}
	private void updateLocationInfo(){
		String locStr = "Location: "+panel.getOrigin().path;
		lblLocationInfo.setText( locStr );		
	}
	
	private void addHandlers(){
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				switch (e.getKeyChar() ){
				case  ' ':
					if ( evaluationThread == null){
						world.evaluate( rule );
						updateFieldInfo();
						panel.update();
					}
					break;
				case 'r':
					setCells( Util.randomField( settings.randomFieldRadius, settings.randomFillPercent ) );
					break;
				case 'd':
					setCells( new Path[0] );
					break;
				case 'x':
					doChangeRule();
					break;
				case 'e':
					doSaveImage();
					break;
				case 's':
					doSaveField();
					break;
				case 'l':
					doLoadField();
					break;
				case 't':
					doEditSettings();
					break;
				case 'u':
					panel.update();
					break;
				case 'z':
					doClusterize();
					break;
				case 'o':
					waypointNavigator.next();
					break;
				case 'p':
					waypointNavigator.previous();
					break;
				}
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				switch( e.getKeyCode()){
				case KeyEvent.VK_UP : panel.offsetView(0, -0.1); break;
				case KeyEvent.VK_DOWN : panel.offsetView(0, 0.1); break;
				case KeyEvent.VK_LEFT : panel.offsetView(0.1, 0); break;
				case KeyEvent.VK_RIGHT : panel.offsetView(-0.1, 0); break;
				case KeyEvent.VK_OPEN_BRACKET : panel.rotateView( -0.06); break;
				case KeyEvent.VK_CLOSE_BRACKET : panel.rotateView( 0.06); break;
				case KeyEvent.VK_C : { panel.centerView(); break;}
				case KeyEvent.VK_A : panel.antiAlias = ! panel.antiAlias; panel.repaint(); break;
				case KeyEvent.VK_G : panel.showGrid = ! panel.showGrid; panel.repaint(); break;
				case KeyEvent.VK_ENTER: toggleRunning(); break;
				}
			}
		});
		panel.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent arg0) {
				try{
					if( arg0.getButton() == MouseEvent.BUTTON1 ){
						Path point = panel.mouse2cellPath(arg0.getX(), arg0.getY());
						if (point != null){
							world.setCell( point, 1 ^ world.getCell( point ) );
							panel.update();
						}else{
							System.err.println("Non-point");
						}
					}else{
						Path point = panel.mouse2cellPathRel(arg0.getX(), arg0.getY());
						//OrientedPath o = new OrientedPath(point, 0);
						panel.rebaseRelative( point );
					}
				}catch( Exception err ){
					System.err.println("Error:"+err.getMessage());
				}
			}});
		panel.AddPoincarePanelListener( new PoincarePanelListener(){
			public void originChanged(PoincarePanelEvent e) {
				updateLocationInfo();
			}});
	}

	/**Find clusters in the field, and set waypoints to them*/
	protected void doClusterize() {
		waypointNavigator.clear();
		System.out.println("Clusterizing...");
		Clusterizer c = null;
		synchronized( world ){
			c = new Clusterizer( world );
		}
		System.out.println("Done. Found "+c.clusters.size()+" clusters");
		for( Clusterizer.Cluster cl: c.clusters ){
			waypointNavigator.add( cl.cells.get(0) );
		}
		waypointNavigator.current();
	}
	protected void doEditSettings() {
		Settings settingsCopy = (Settings)(settings.clone());
		SettingsDialog sd = new SettingsDialog(settingsCopy);
		if ( sd.showDialog() ){
			settings = settingsCopy;
		}
	}

	protected void toggleRunning() {
		if( evaluationThread == null )
			startEvaluation();
		else
			stopEvaluation();
	}

	protected void doChangeRule() {
		String sRule = JOptionPane.showInputDialog(this, "Enter the rule in format B3/S23", rule.getCode());
		if ( sRule != null ){
			try{
				setRule( sRule );
			}catch( RuleSyntaxException err ){
				JOptionPane.showMessageDialog(this, err.getMessage(), "Error parsing rule", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	public void setRule( TotalisticRule r){
		rule = r;
		updateFieldInfo();
		System.out.println( "Rule set to "+rule);
	}
	
	public void setRule( String sRule ) throws RuleSyntaxException{
		stopEvaluation();
		Rule parsed = Rule.parseRule(sRule);
		if (parsed.isVacuumStable() ){
			//normal rule
			setRule( parsed );
		}else{
			if ( parsed.isValidDayNight() ){
				DayNightRule converted = new DayNightRule(parsed);
				System.out.println( "Converting Day/Night rule "+parsed+" to the pair of stable rules:"+ converted);
				assert( converted.isValidRule() );
				setRule(converted);
			}else
				throw new RuleSyntaxException("Rule "+parsed+" is not supported: it has B0 and SA. Try inverted rule:"+parsed.invertBoth());
		}
	}
	
	public void setCells( Path[] cells ){
		boolean wasRunning = false;
		if( evaluationThread != null ) {
			stopEvaluation();
			wasRunning = true;
		}
		world.setCells( cells );
		world.setFieldState(0);
		panel.update();
		updateFieldInfo();
		if (wasRunning) startEvaluation();
	}
	
	public MainFrame() {
		super("Hyperbolic CA simulator");
		createUI();
		addHandlers();
	}
	
	public static void main(String[] args) {
		MainFrame frm = new MainFrame();

		Path root = Path.getRoot(); 
		
		Path[] cells = PathNavigation.neigh10( root ); 
		frm.setCells( cells );
		
		frm.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frm.setSize( 600, 600 );
		frm.setVisible( true );
	}
	
	public void startEvaluationFast(){
		if( evaluationThread != null ) return;
		evaluationThread = new EvaluationRunner(world, rule, this);
		evaluationThread.setDelayMs(0);
		evaluationThread.start();		
	}
	public void startEvaluation(){
		if( evaluationThread != null ) return;
		evaluationThread = new EvaluationRunner(world, rule, this);
		evaluationThread.start();
	}
	public void stopEvaluation(){
		if (evaluationThread != null){
			evaluationThread.requestStop();
			try {
				evaluationThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			evaluationThread = null;
		}
	}

	@Override
	public void notifyUpdate( Object w ) {
		//Receive results from the evaluator
		//This code is called from the evaluator thread
		if (w == world){
			SwingUtilities.invokeLater(new Runnable(){
				@Override
				public void run() {
					panel.update();
					updateFieldInfo();
				}
			});
		}else{
			System.err.println("Unknown notification");
		}
	}
	
	private JFileChooser fieldFileChooser = null;
	private JFileChooser imageFileChooser = null;
	private void doSaveImage() {
		if ( imageFileChooser == null ){
			imageFileChooser = new JFileChooser();
			FileFilter filter = new FileNameExtensionFilter("PNG image (*.png)", "png");
			imageFileChooser.addChoosableFileFilter( filter );
			imageFileChooser.setFileFilter( filter );
		}
		
		if (imageFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = imageFileChooser.getSelectedFile();
            if ( ! file.getName().contains("."))
            	file = new File( file.getParentFile(), file.getName()+".png");
            try {
            	int s = settings.exportImageSize;
				ImageIO.write( panel.exportImage(new Dimension(s, s), settings.exportAntiAlias ),
						"PNG", file);
			} catch (IOException err) {
				JOptionPane.showMessageDialog(this, err.getMessage(), "Can not save file", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	private void saveFieldData( File f, Field fld ) throws FileNotFoundException, IOException{
		ObjectOutputStream oos = new ObjectOutputStream( new GZIPOutputStream( new FileOutputStream( f )));
		
		oos.writeObject( fld );
		oos.writeObject( rule);
		oos.close();
	}
	private Pair<SimpleMapField, TotalisticRule> loadFieldData( File f ) throws FileNotFoundException, IOException, ClassNotFoundException{
		ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream( f )));
		Object o = ois.readObject();
		Object rule = ois.readObject();
		ois.close();
		try{
			return new Pair<SimpleMapField, TotalisticRule>( (SimpleMapField)o, (TotalisticRule) rule );
		}catch(ClassCastException e){
			throw new IOException("File has wrong format");
		}
	}
	
	private void ensureSaveFileChooser(){
		if (fieldFileChooser != null ) return;
		fieldFileChooser = new JFileChooser();
		FileFilter filter = new FileNameExtensionFilter("Gzipped serialized Java object (*.sgz)", "sgz");
		fieldFileChooser.addChoosableFileFilter(filter);
		fieldFileChooser.setFileFilter( filter );
	}
	private void doSaveField(){
		ensureSaveFileChooser();
		if (fieldFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fieldFileChooser.getSelectedFile();
            if ( ! file.getName().contains("."))
            	file = new File( file.getParentFile(), file.getName()+".sgz");
            try {
				saveFieldData(file, world);
			} catch (Exception err) {
				JOptionPane.showMessageDialog(this, err.getMessage(), "Error writing file", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	private void doLoadField(){
		ensureSaveFileChooser();
		if ( fieldFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fieldFileChooser.getSelectedFile();
            try {
				Pair<SimpleMapField, TotalisticRule> world_rule= loadFieldData(file);
				setWorld( world_rule.left );
				setRule( world_rule.right );
			} catch (Exception err) {
				JOptionPane.showMessageDialog(this, err.getMessage(), "Can not load file", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void setWorld(SimpleMapField newWorld) {
		assert newWorld != null;
		stopEvaluation();
		world = newWorld;
		panel.setField( newWorld );
		updateFieldInfo();
	}
}
