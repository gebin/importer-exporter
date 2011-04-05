/*
 * This file is part of the 3D City Database Importer/Exporter.
 * Copyright (c) 2007 - 2011
 * Institute for Geodesy and Geoinformation Science
 * Technische Universitaet Berlin, Germany
 * http://www.gis.tu-berlin.de/
 *
 * The 3D City Database Importer/Exporter program is free software:
 * you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program. If not, see 
 * <http://www.gnu.org/licenses/>.
 * 
 * The development of the 3D City Database Importer/Exporter has 
 * been financially supported by the following cooperation partners:
 * 
 * Business Location Center, Berlin <http://www.businesslocationcenter.de/>
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * Berlin Senate of Business, Technology and Women <http://www.berlin.de/sen/wtf/>
 */
package de.tub.citydb.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import de.tub.citydb.components.citygml.exporter.CityGMLExportPlugin;
import de.tub.citydb.components.citygml.importer.CityGMLImportPlugin;
import de.tub.citydb.components.database.DatabasePlugin;
import de.tub.citydb.components.kml.KMLExportPlugin;
import de.tub.citydb.components.matching.MatchingPlugin;
import de.tub.citydb.components.preferences.PreferencesPlugin;
import de.tub.citydb.config.Config;
import de.tub.citydb.config.ConfigUtil;
import de.tub.citydb.config.gui.GuiConfigUtil;
import de.tub.citydb.config.gui.window.MainWindow;
import de.tub.citydb.config.gui.window.WindowSize;
import de.tub.citydb.config.internal.Internal;
import de.tub.citydb.config.project.ProjectConfigUtil;
import de.tub.citydb.config.project.global.LanguageType;
import de.tub.citydb.database.DBConnectionPool;
import de.tub.citydb.gui.console.ConsoleWindow;
import de.tub.citydb.gui.menubar.MenuBar;
import de.tub.citydb.gui.util.GuiUtil;
import de.tub.citydb.log.Logger;
import de.tub.citydb.plugin.api.Plugin;
import de.tub.citydb.plugin.api.controller.ViewController;
import de.tub.citydb.plugin.api.extension.view.View;
import de.tub.citydb.plugin.api.extension.view.ViewExtension;
import de.tub.citydb.plugin.internal.InternalPlugin;
import de.tub.citydb.plugin.service.PluginService;

@SuppressWarnings("serial")
public class ImpExpGui extends JFrame implements ViewController, PropertyChangeListener {
	private final Logger LOG = Logger.getInstance();

	private final Config config;
	private final JAXBContext jaxbProjectContext;
	private final JAXBContext jaxbGuiContext;
	private final PluginService pluginService;
	private final DBConnectionPool dbPool;

	private JPanel main;
	private JTextArea consoleText;
	private JLabel statusText;
	private JLabel connectText;
	private MenuBar menuBar;
	private JTabbedPane menu;
	private JSplitPane splitPane;
	private JPanel console;
	private JLabel consoleLabel;
	private ConsoleWindow consoleWindow;
	private int tmpConsoleWidth;
	private int activePosition;

	private List<View> views;
	private PreferencesPlugin preferencesPlugin;
	
	private PrintStream out;
	private PrintStream err;

	// internal state
	private LanguageType currentLang = null;

	// set look & feel
	{
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public ImpExpGui(JAXBContext jaxbProjectContext,
			JAXBContext jaxbGuiContext,
			PluginService pluginService,
			Config config) {
		this.jaxbProjectContext = jaxbProjectContext;
		this.jaxbGuiContext = jaxbGuiContext;
		this.pluginService = pluginService;
		this.config = config;

		dbPool = DBConnectionPool.getInstance();
		dbPool.addPropertyChangeListener(DBConnectionPool.PROPERTY_DB_IS_CONNECTED, this);
		
		// required for preferences plugin
		consoleText = new JTextArea();
	}

	public void invoke(List<String> errMsgs) {
		// init GUI elements
		initGui();
		doTranslation();
		showWindow();

		// initConsole;
		initConsole();

		if (!errMsgs.isEmpty()) {
			for (String msg : errMsgs)
				LOG.error(msg);
			LOG.info("Project settings initialized using default values.");
		}		
	}

	public void restoreDefaults() {
		if (consoleWindow.isVisible() != config.getGui().getConsoleWindow().isDetached())
			enableConsoleWindow(config.getGui().getConsoleWindow().isDetached(), false);

		consoleWindow.setSize(0, 0);
		showWindow();
	}

	private void initGui() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		activePosition = 0;
		main = new JPanel();

		menuBar = new MenuBar(config, jaxbProjectContext, this);
		setJMenuBar(menuBar);
		
		console = new JPanel();
		consoleLabel = new JLabel();
		consoleText.setAutoscrolls(true);
		consoleText.setFont(new Font(Font.MONOSPACED, 0, 11));
		consoleText.setEditable(false);
		consoleWindow = new ConsoleWindow(console, config, this);

		statusText = new JLabel();
		connectText = new JLabel();

		Border border = BorderFactory.createEtchedBorder();
		Border margin = BorderFactory.createEmptyBorder(0, 2, 0, 2);
		statusText.setBorder(new CompoundBorder(border, margin));
		statusText.setOpaque(true);
		statusText.setBackground(new Color(255,255,255));		

		connectText.setBorder(new CompoundBorder(border, margin));
		connectText.setBackground(new Color(255,255,255));
		connectText.setOpaque(true);
		
		views = new ArrayList<View>();
		preferencesPlugin = pluginService.getInternalPlugin(PreferencesPlugin.class);
		views.add(pluginService.getInternalPlugin(CityGMLImportPlugin.class).getView());
		views.add(pluginService.getInternalPlugin(CityGMLExportPlugin.class).getView());
		views.add(pluginService.getInternalPlugin(KMLExportPlugin.class).getView());
		views.add(pluginService.getInternalPlugin(MatchingPlugin.class).getView());

		for (Plugin plugin : pluginService.getExternalPlugins()) {
			if (plugin instanceof ViewExtension) {
				ViewExtension extension = (ViewExtension)plugin;
				if (extension.getView() != null)				
					views.add(((ViewExtension)plugin).getView());
			}
		}

		views.add(pluginService.getInternalPlugin(DatabasePlugin.class).getView());
		views.add(preferencesPlugin.getView());

		menu = new JTabbedPane();
		int index = 0;
		for (View view : views)
			menu.insertTab(view.getTitle(), view.getIcon(), view.getViewComponent(), view.getToolTip(), index++);

		menu.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (menu.getSelectedIndex() == activePosition) 
					return;

				if (menu.getComponentAt(activePosition) == preferencesPlugin.getView().getViewComponent()) {
					if (!preferencesPlugin.requestChange())
						menu.setSelectedIndex(activePosition);
				}

				activePosition = menu.getSelectedIndex();
			}
		});

		addWindowListener(new WindowListener() {
			public void windowClosed(WindowEvent e) {
				shutdown();
			}

			public void windowActivated(WindowEvent e) {}
			public void windowClosing(WindowEvent e) {}
			public void windowDeactivated(WindowEvent e) {}
			public void windowDeiconified(WindowEvent e) {}
			public void windowIconified(WindowEvent e) {}
			public void windowOpened(WindowEvent e) {}
		});

		//layout
		setIconImage(Toolkit.getDefaultToolkit().getImage(ImpExpGui.class.getResource("/resources/img/logo_small.png")));
		setLayout(new GridBagLayout());

		// main panel
		main.setBorder(BorderFactory.createEmptyBorder());
		main.setBackground(this.getBackground());
		main.setLayout(new GridBagLayout());
		{
			main.add(menu, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,5,5,5,5));
			JPanel status = new JPanel();
			status.setBorder(BorderFactory.createEmptyBorder());
			status.setBackground(this.getBackground());
			main.add(status, GuiUtil.setConstraints(0,1,1.0,0.0,GridBagConstraints.HORIZONTAL,5,5,0,5));
			status.setLayout(new GridBagLayout());
			{
				status.add(statusText, GuiUtil.setConstraints(0,0,1.0,0.0,GridBagConstraints.HORIZONTAL,0,0,0,2));
				status.add(connectText, GuiUtil.setConstraints(1,0,0.0,0.0,GridBagConstraints.HORIZONTAL,0,2,0,0));
			}
		}

		// console panel
		console.setBorder(BorderFactory.createEmptyBorder());
		console.setBackground(this.getBackground());
		console.setLayout(new GridBagLayout());
		{
			console.add(consoleLabel, GuiUtil.setConstraints(0,0,0.0,0.0,GridBagConstraints.BOTH,5,5,0,5));
			JScrollPane scroll = new JScrollPane(consoleText);
			scroll.setBorder(BorderFactory.createEtchedBorder());
			console.add(scroll, GuiUtil.setConstraints(0,1,1.0,1.0,GridBagConstraints.BOTH,0,5,5,5));
		}

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setContinuousLayout(true);
		splitPane.setBorder(BorderFactory.createEmptyBorder());
		splitPane.setOpaque(false);
		splitPane.setUI(new BasicSplitPaneUI() {
			public BasicSplitPaneDivider createDefaultDivider() {
				return new BasicSplitPaneDivider(this) {
					public void setBorder(Border b) {
					}
				};
			}
		});

		splitPane.setLeftComponent(main);
		splitPane.setRightComponent(console);
		add(splitPane, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,0,0,0,0));
	}

	private void showWindow() {
		WindowSize size = config.getGui().getMainWindow().getSize();

		Toolkit t = Toolkit.getDefaultToolkit();
		Insets frame_insets = t.getScreenInsets(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
		int frame_insets_x = frame_insets.left + frame_insets.right;
		int frame_insets_y = frame_insets.bottom + frame_insets.top;

		// derive virtual bounds of multiple screen layout
		Rectangle virtualBounds = new Rectangle();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();

		for (int j = 0; j < gs.length; j++) { 
			GraphicsDevice gd = gs[j];
			GraphicsConfiguration[] gc = gd.getConfigurations();
			for (int i=0; i < gc.length; i++)
				virtualBounds = virtualBounds.union(gc[i].getBounds());
		}

		Integer x = size.getX();
		Integer y = size.getY();
		Integer width = size.getWidth();
		Integer height = size.getHeight();
		Integer dividerLocation = config.getGui().getMainWindow().getDividerLocation();

		// create default values for main window
		if (x == null || y == null || width == null || height == null || 
				!virtualBounds.contains(x , y, frame_insets_x != 0 ? frame_insets_x : 40, frame_insets_y != 0 ? frame_insets_y : 40)) {
			Dimension dim = t.getScreenSize();
			int user_insets_x = 100;
			int user_insets_y = 50;

			width = dim.width - frame_insets_x - user_insets_x;
			height = dim.height - frame_insets_y - user_insets_y;
			x = user_insets_x / 2 + frame_insets.left;
			y = user_insets_y / 2 + frame_insets.top;

			// if console is detached, also create default values for console window
			if (config.getGui().getConsoleWindow().isDetached()) {
				x -= 15;
				width = width / 2 + 30;
				consoleWindow.setLocation(x + width, y);
				consoleWindow.setSize(width - 30, height);
			} else
				dividerLocation = (int)(width * .5) + 20;
		}

		setLocation(x, y);
		setSize(width, height);
		setVisible(true);

		if (!config.getGui().getConsoleWindow().isDetached())
			main.setPreferredSize(new Dimension((int)(width * .5) + 20, 1));

		if (dividerLocation != null && dividerLocation > 0 && dividerLocation < width)
			splitPane.setDividerLocation(dividerLocation);
	}

	private void initConsole() {
		Charset encoding;

		try {
			encoding = Charset.forName("UTF-8");
		} catch (Exception e) {
			encoding = Charset.defaultCharset();
		}

		// let standard out point to console
		JTextAreaOutputStream jTextwriter = new JTextAreaOutputStream(consoleText, new ByteArrayOutputStream(), encoding);
		PrintStream writer;

		try {
			writer = new PrintStream(jTextwriter, true, encoding.displayName());
		} catch (UnsupportedEncodingException e) {
			writer = new PrintStream(jTextwriter);
		}

		out = System.out;
		err = System.err;

		System.setOut(writer);
		System.setErr(writer);

		// show console window if required
		if (config.getGui().getConsoleWindow().isDetached()) {
			enableConsoleWindow(true, false);
			requestFocus();
		}
	}

	public void loadSettings() {
		for (InternalPlugin plugin : pluginService.getInternalPlugins())
			plugin.loadSettings();
	}

	public void setSettings() {
		for (InternalPlugin plugin : pluginService.getInternalPlugins())
			plugin.setSettings();
	}

	public void setLoggingSettings() {
		preferencesPlugin.setLoggingSettings();
	}

	public void doTranslation () {
		try {
			LanguageType lang = config.getProject().getGlobal().getLanguage();
			if (lang == currentLang)
				return;

			Locale locale = new Locale(lang.value());
			Internal.I18N = ResourceBundle.getBundle("de.tub.citydb.gui.Label", locale);
			currentLang = lang;

			setTitle(Internal.I18N.getString("main.window.title"));

			int index = 0;
			for (View view : views)
				menu.setTitleAt(index++, view.getTitle());

			statusText.setText(Internal.I18N.getString("main.status.ready.label"));

			if (dbPool.isConnected())
				connectText.setText(Internal.I18N.getString("main.status.database.connected.label"));
			else
				connectText.setText(Internal.I18N.getString("main.status.database.disconnected.label"));

			menuBar.doTranslation();
			for (Plugin plugin : pluginService.getPlugins())
				plugin.switchLocale(locale);
			
			consoleLabel.setText(Internal.I18N.getString("main.label.console"));
		}
		catch (MissingResourceException e) {
			LOG.error("Missing resource: " + e.getKey());
		}
	}

	public void errorMessage(String title, String text) {
		JOptionPane.showMessageDialog(this, text, title, JOptionPane.ERROR_MESSAGE);
	}

	public void enableConsoleWindow(boolean enable, boolean resizeMain) {
		splitPane.setEnabled(!enable);

		if (enable) {
			if (resizeMain) {
				tmpConsoleWidth = console.getWidth();
				if (getExtendedState() == JFrame.MAXIMIZED_BOTH) {
					Toolkit t = Toolkit.getDefaultToolkit();
					Insets insets = t.getScreenInsets(getGraphicsConfiguration());
					Rectangle bounds = getGraphicsConfiguration().getBounds();
					setLocation(bounds.x, bounds.y);
					setSize(bounds.width - insets.left - tmpConsoleWidth - insets.right, 
							bounds.height - insets.top - insets.bottom);					
				} else				
					setSize(getWidth() - tmpConsoleWidth, getHeight());
			}

			consoleWindow.activate();			
		} else {
			consoleWindow.dispose();

			int width = tmpConsoleWidth;
			if (width == 0)
				width = consoleWindow.getWidth();

			if (resizeMain) {		
				setSize(getWidth() + width, getHeight());
			}

			width = main.getWidth();
			int dividerLocation = splitPane.getDividerLocation();
			splitPane.setRightComponent(console);
			main.setPreferredSize(new Dimension(width, 1));
			splitPane.setDividerLocation(dividerLocation);
		}
	}

	public void connectToDatabase() {
		// TODO
		//databaseView.connect();
	}

	public void disconnectFromDatabase() {
		// TODO
		//databaseView.disconnect();
	}

	public boolean saveProjectSettings() {
		String configPath = ConfigUtil.createConfigPath(config.getInternal().getConfigPath());

		if (configPath == null) {
			String text = Internal.I18N.getString("common.dialog.error.io.configPath");
			Object[] args = new Object[]{ config.getInternal().getConfigPath() };
			String result = MessageFormat.format(text, args);

			errorMessage(Internal.I18N.getString("common.dialog.error.io.title"), result);
			return false;
		}

		String projectConf = configPath + File.separator + config.getInternal().getConfigProject();

		try {
			ProjectConfigUtil.marshal(config.getProject(), projectConf, jaxbProjectContext);
		} catch (JAXBException jaxbE) {
			errorMessage(Internal.I18N.getString("common.dialog.error.io.title"), 
					Internal.I18N.getString("common.dialog.error.io.general"));
			return false;
		}

		return true;
	}

	private boolean saveGUISettings() {
		String configPath = ConfigUtil.createConfigPath(config.getInternal().getConfigPath());

		if (configPath == null) {
			String text = Internal.I18N.getString("common.dialog.error.io.configPath");
			Object[] args = new Object[]{ config.getInternal().getConfigPath() };
			String result = MessageFormat.format(text, args);

			errorMessage(Internal.I18N.getString("common.dialog.error.io.title"), result);
			return false;
		}

		String guiConf = configPath + File.separator + config.getInternal().getConfigGui();

		// set window size
		Rectangle rect = getBounds();
		MainWindow window = config.getGui().getMainWindow();
		window.getSize().setX(rect.x);
		window.getSize().setY(rect.y);
		window.getSize().setWidth(rect.width);
		window.getSize().setHeight(rect.height);
		window.setDividerLocation(splitPane.getDividerLocation());

		// set console window size
		consoleWindow.setSettings();

		try {
			GuiConfigUtil.marshal(config.getGui(), guiConf, jaxbGuiContext);
		} catch (JAXBException jaxbE) {
			errorMessage(Internal.I18N.getString("common.dialog.error.io.title"), 
					Internal.I18N.getString("common.dialog.error.io.general"));
			return false;
		}

		return true;
	}

	public void setStatusText(String message) {
		statusText.setText(message);
	}

	public void clearConsole() {
		consoleText.setText("");
	}

	public JTextArea getConsole() {
		return consoleText;
	}

	private void shutdown() {		
		System.setOut(out);
		System.setErr(err);
		boolean success = true;

		consoleWindow.dispose();

		if (dbPool.isConnected()) {
			LOG.info("Terminating database connection");
			try {
				dbPool.disconnect();
			} catch (SQLException e) {
				LOG.error("Failed to terminate database connection: " + e.getMessage());
				success = false;
			}
		}

		// shutdown plugins
		if (pluginService.getExternalPlugins().isEmpty())
			LOG.info("Shutting down plugins");
		
		for (Plugin plugin : pluginService.getPlugins())
			plugin.shutdown();
		
		LOG.info("Saving project settings");
		saveProjectSettings();
		saveGUISettings();

		if (success)
			LOG.info("Application successfully terminated");
		else {
			LOG.info("Application did not terminate normally");
			System.exit(1);
		}
	}

	private class JTextAreaOutputStream extends FilterOutputStream {
		private final int MAX_DOC_LENGTH = 10000;
		private final JTextArea ta;
		private final Charset encoding;

		public JTextAreaOutputStream (JTextArea ta, OutputStream stream, Charset encoding) {
			super(stream);
			this.ta = ta;
			this.encoding = encoding;
		}

		@Override
		public void write(final byte[] b) {
			try {
				ta.append(new String(b, encoding));
			} catch (Error e) {
				//
			}

			flush();
		}

		@Override
		public void write(final byte b[], final int off, final int len) {
			try {
				ta.append(new String(b, off, len, encoding));
			} catch (Error e) {
				//
			} 

			flush();
		}

		public void flush() {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					ta.setCaretPosition(ta.getDocument().getLength());	
					if (ta.getLineCount() > MAX_DOC_LENGTH)
						ta.setText("...truncating console output after " + MAX_DOC_LENGTH + " log messages...");
				}
			});
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(DBConnectionPool.PROPERTY_DB_IS_CONNECTED)) {
			if (!(Boolean)evt.getNewValue())
				connectText.setText(Internal.I18N.getString("main.status.database.disconnected.label"));
			else
				connectText.setText(Internal.I18N.getString("main.status.database.connected.label"));
		}
	}
}
