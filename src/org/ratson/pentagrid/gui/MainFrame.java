package org.ratson.pentagrid.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;
import javax.management.RuntimeErrorException;
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
import org.ratson.pentagrid.json.FileFormatException;
import org.ratson.pentagrid.json.GSONSerializer;
import org.ratson.util.Pair;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

@SuppressWarnings("serial")
public class MainFrame extends JFrame implements NotificationReceiver {
	
	private SimpleMapField world = new SimpleMapField();
	private TotalisticRule rule = new Rule(new int[]{3}, new int[]{2,3});
	private FarPoincarePanel panel;
	private EvaluationRunner evaluationThread=null;
	private Settings settings = new Settings();
	private JLabel lblFieldInfo, lblLocationInfo;
	private WaypointNavigator waypointNavigator=new WaypointNavigator();
	protected Point dragOrigin=null;
	protected Point lastDragPoint=null;

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
				case 'a':
					doExportAnimation();
					break;
				case 'm':
					doExportAnimationMovement();
					break;
				}
			}
			

			@Override
			public void keyPressed(KeyEvent e) {
				
				switch( e.getKeyCode()){
				case KeyEvent.VK_UP : panel.offsetView(0, -settings.offsetVelocity); break;
				case KeyEvent.VK_DOWN : panel.offsetView(0, settings.offsetVelocity); break;
				case KeyEvent.VK_LEFT : panel.offsetView(settings.offsetVelocity, 0); break;
				case KeyEvent.VK_RIGHT : panel.offsetView(-settings.offsetVelocity, 0); break;
				case KeyEvent.VK_OPEN_BRACKET : panel.rotateView( -settings.rotationVelocity); break;
				case KeyEvent.VK_CLOSE_BRACKET : panel.rotateView( settings.rotationVelocity); break;
				case KeyEvent.VK_C : { panel.centerView(); break;}
				case KeyEvent.VK_A : panel.antiAlias = ! panel.antiAlias; panel.repaint(); break;
				case KeyEvent.VK_G : panel.showGrid = ! panel.showGrid; panel.repaint(); break;
				case KeyEvent.VK_ENTER: toggleRunning(); break;
				}
			}
		});
		
		MouseAdapter listener = (new MouseAdapter(){
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
					}else if (arg0.getButton() == MouseEvent.BUTTON3 ){
						System.out.println("Drag started");
						lastDragPoint = dragOrigin = arg0.getPoint();
						
					}
				}catch( Exception err ){
					System.err.println("Error:"+err.getMessage());
				}
			}
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				panel.rotateView( settings.rotationVelocity * e.getPreciseWheelRotation());
			}
			@Override
			public void mouseDragged(MouseEvent e) {
				if (dragOrigin != null){
					double scale = panel.getScale();
					double dx = (e.getX() - lastDragPoint.getX()) / scale;
					double dy = -(e.getY() - lastDragPoint.getY()) / scale;
					lastDragPoint = e.getPoint();
					panel.offsetView(dx, dy);
				}
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				dragOrigin = null;
			}
			});
		
		panel.AddPoincarePanelListener( new PoincarePanelListener(){
			public void originChanged(PoincarePanelEvent e) {
				updateLocationInfo();
			}});
		panel.addMouseListener(listener);
		panel.addMouseMotionListener(listener);
		panel.addMouseWheelListener(listener);
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
		createImageChooser();
		if (imageFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = imageFileChooser.getSelectedFile();
            if ( ! file.getName().contains("."))
            	file = new File( file.getParentFile(), file.getName()+".png");
            try {
            	this.saveFieldImageAs(file);
			} catch (IOException err) {
				JOptionPane.showMessageDialog(this, err.getMessage(), "Can not save file", JOptionPane.ERROR_MESSAGE);
			}
		}
	}	
	private void saveFieldGson( File f, Field fld ) throws IOException{
		GZIPOutputStream gzout = new GZIPOutputStream( new FileOutputStream( f ));
		JsonWriter jsw = new JsonWriter(new OutputStreamWriter(gzout));
		try{
			GSONSerializer.writeField( jsw, fld, rule );
		}finally{
			jsw.close();
			gzout.close();
		}
	}
	private Pair<SimpleMapField, TotalisticRule> loadFieldGson( File f ) throws FileNotFoundException, IOException, FileFormatException{
		GZIPInputStream gzout = new GZIPInputStream( new FileInputStream( f ));
		JsonReader jsr = new JsonReader(new InputStreamReader(gzout));
		try{
			Pair<SimpleMapField, TotalisticRule> rval = GSONSerializer.readField( jsr );
			return rval;
		}finally{
			jsr.close();
			gzout.close();
		}
	}
	private void ensureSaveFileChooser(){
		if (fieldFileChooser != null ) return;
		fieldFileChooser = new JFileChooser();
		FileFilter filter = new FileNameExtensionFilter("Gzipped JSON object (*.jsongz)", "jsongz");
		fieldFileChooser.addChoosableFileFilter(filter);
		fieldFileChooser.setFileFilter( filter );
	}
	private void doSaveField(){
		ensureSaveFileChooser();
		if (fieldFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fieldFileChooser.getSelectedFile();
            if ( ! file.getName().contains("."))
            	file = new File( file.getParentFile(), file.getName()+".jsongz");
            try {
            	saveFieldGson(file, world);
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
            	Pair<SimpleMapField, TotalisticRule> world_rule= loadFieldGson(file);
				setWorld( world_rule.left );
				setRule( world_rule.right );
			} catch (Exception err) {
				//err.printStackTrace();
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

	private void createImageChooser(){
		if ( imageFileChooser == null ){
			imageFileChooser = new JFileChooser();
			FileFilter filter = new FileNameExtensionFilter("PNG image (*.png)", "png");
			imageFileChooser.addChoosableFileFilter( filter );
			imageFileChooser.setFileFilter( filter );
		}
		
	}
	private void doExportAnimation() {
		createImageChooser();
		if (imageFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = imageFileChooser.getSelectedFile();
                if ( ! file.getName().contains("."))
                	file = new File( file.getParentFile(), file.getName()+".png");
                
                String sframes = JOptionPane.showInputDialog(this, "Enter numer of frames");
                int nFrames = Integer.parseInt(sframes);
                if (nFrames < 0 || nFrames > 10000)
                	throw new RuntimeException("Incorrect number of frames");
                
                exportAnimation( file.getParentFile(), file.getName(),  nFrames);
			} catch (IOException err) {
				JOptionPane.showMessageDialog(this, err.getMessage(), "Can not save file", JOptionPane.ERROR_MESSAGE);
			} catch (Exception err){
				JOptionPane.showMessageDialog(this, err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);				
			}
		}
	}
	
	private void doExportAnimationMovement() {
		createImageChooser();
		if (imageFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = imageFileChooser.getSelectedFile();
                if ( ! file.getName().contains("."))
                	file = new File( file.getParentFile(), file.getName()+".png");
                
                int nFrames = Integer.parseInt(JOptionPane.showInputDialog(this, "Enter numer of steps"));
                int nFramesPerGen = Integer.parseInt(JOptionPane.showInputDialog(this, "Enter number of frames per generation"));
                double nOffsetPerGen = Double.parseDouble(JOptionPane.showInputDialog(this, "Enter offset per generation"));
                
                
                if (nFrames < 0 || nFrames > 10000)
                	throw new RuntimeException("Incorrect number of frames");
                
                exportAnimationMovement( file.getParentFile(), file.getName(),  nFrames, nFramesPerGen, nOffsetPerGen*1.061275061905); //this constant is length of the pentagon side
			} catch (IOException err) {
				JOptionPane.showMessageDialog(this, err.getMessage(), "Can not save file", JOptionPane.ERROR_MESSAGE);
			} catch (Exception err){
				JOptionPane.showMessageDialog(this, err.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);				
			}
		}
	}
	
	private void exportAnimationMovement(File folder, String name,
			int nFrames, int nFramesPerGen, double nOffsetPerGen) throws IOException {
		int dotPos = name.lastIndexOf('.');
		
		String baseName=null, ext=null;
		
		if (dotPos != -1){
			baseName = name.substring(0, dotPos);
			ext = name.substring(dotPos);
		}else{
			baseName = name;
			ext="";
		}
		
		int frame = 0;
		
		for(int generation=0; generation< nFrames; ++generation){
			for(int iSubFrame=0; iSubFrame<nFramesPerGen; ++iSubFrame, ++frame){
				File frameFile = new File(folder, baseName + String.format("%04d", frame) + ext);
				System.err.println("Writing "+frameFile);
				saveFieldImageAs( frameFile );
				
				panel.offsetView(0, nOffsetPerGen/nFramesPerGen);
				panel.update();			
			}
			world.evaluate( rule );
			updateFieldInfo();
			panel.update();			
		}
		
	}
	private void exportAnimation(File folder, String name, int nFrames) throws IOException {
		int dotPos = name.lastIndexOf('.');
		String baseName=null, ext=null;
		if (dotPos != -1){
			baseName = name.substring(0, dotPos);
			ext = name.substring(dotPos);
		}else{
			baseName = name;
			ext="";
		}
		for(int frame=0; frame < nFrames; ++frame){
			File frameFile = new File(folder, baseName + String.format("%04d", frame) + ext);
			System.err.println("Writing "+frameFile);
			saveFieldImageAs( frameFile );
			
			world.evaluate( rule );
			updateFieldInfo();
			panel.update();			
		}
	}
	
	private void saveFieldImageAs(File f) throws IOException{
		int dotPos = f.getName().lastIndexOf('.');
		String format;
		if (dotPos == -1){
			format="PNG";
		}else{
			format = f.getName().substring(dotPos+1).toUpperCase();
		}
		
    	int s = settings.exportImageSize;
		ImageIO.write( panel.exportImage(new Dimension(s, s), settings.exportAntiAlias ),
				format, f);
		
	}
}
