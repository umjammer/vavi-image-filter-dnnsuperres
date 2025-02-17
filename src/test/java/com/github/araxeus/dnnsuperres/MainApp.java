package com.github.araxeus.dnnsuperres;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.google.common.base.Stopwatch;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NativeFileDialog;
import org.rococoa.Foundation;


public class MainApp {

    Preferences prefs = Preferences.userNodeForPackage(MainApp.class);

    private static final Logger logger = Logger.getLogger(MainApp.class.getName());

    public static final Color SVGBLUE = new Color(115, 208, 244),
            SCARLET = new Color(255, 36, 0),
            LIGHT = new Color(194, 236, 255),
            DARK = new Color(17, 19, 19);
    private JFrame frame;
    private JSplitPane upperSplitPane;
    private JTextPane console;
    private JTabbedPane tabbedPane;

    private ButtonGroup mode;

    private JButton startButton,
            loadButton,
            saveButton;
    private JRadioButton btnLapSRNx8;
    private final Icon whiteLoadingGIF,
            blackLoadingGIF;

    private FlatSVGIcon startSVG,
            loadSVG,
            loadOkSVG,
            saveSVG,
            saveOkSVG;

    private MouseAdapter skinChanger;

    private String loadPath,
            savePath;

    private long clickTimer = 0;

    /**
     * Launch the application.
     *
     * @wbp.parser.entryPoint
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                MainApp window = new MainApp();

                logger.addHandler(new MyLogHandler((l, m) -> {
                    switch (l.intValue()) {
                    default:
                    case 500:
                        window.write(m, null);
                        break;
                    case 800:
                        window.write(m, MainApp.SVGBLUE);
                        break;
                    case 900:
                    case 1000:
                        window.write(m, MainApp.SCARLET);
                        break;
                    }
                }));

                window.setSelected();
                window.startTabbedPanel();
                window.frame.setVisible(true);
                window.console.grabFocus();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public MainApp() {
        JFrame.setDefaultLookAndFeelDecorated(true); //custom window decoration
        setSkin(false);
        UIManager.put("TabbedPane.showTabSeparators", true);
        whiteLoadingGIF = new ImageIcon(MainApp.class.getClassLoader().getResource("loadingWHITE.gif"));
        blackLoadingGIF = new ImageIcon(MainApp.class.getClassLoader().getResource("loadingBLACK.gif"));
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setIconImage((new ImageIcon(MainApp.class.getClassLoader().getResource("Icon.png")).getImage()));
        frame.setAutoRequestFocus(true);
        frame.setMinimumSize(new Dimension(500, 180));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS));
        //set default appearance to middle of the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((screenSize.width - frame.getBounds().width) / 2,
                (screenSize.height - frame.getBounds().height) / 2);

        mode = new ButtonGroup();

        JSplitPane mainSplitPane = new JSplitPane();
        mainSplitPane.setEnabled(true);
        mainSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setResizeWeight(0.55);

        frame.getContentPane().add(mainSplitPane);
        //console
        console = new JTextPane();
        console.setEditable(false);
        console.setMinimumSize(new Dimension(150, 100));
        skinChanger = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (clickTimer == 0) {
                    clickTimer = System.currentTimeMillis();
                    return;
                }
                if (System.currentTimeMillis() - clickTimer < 600)
                    SwingUtilities.invokeLater(() -> setSkin(true));

                clickTimer = 0;
            }
        };
        console.addMouseListener(skinChanger);

        JScrollPane scrollPane = new JScrollPane(console);
        scrollPane.setMinimumSize(new Dimension(150, 100));
        mainSplitPane.setRightComponent(scrollPane);

        upperSplitPane = new JSplitPane();
        upperSplitPane.setMinimumSize(new Dimension(400, 90));
        upperSplitPane.setEnabled(false);
        upperSplitPane.setResizeWeight(1.0);
        mainSplitPane.setLeftComponent(upperSplitPane);

        JSplitPane leftSplitPane = new JSplitPane();
        mainSplitPane.setMinimumSize(new Dimension(315, 75));
        leftSplitPane.setEnabled(false);
        leftSplitPane.setResizeWeight(0.7);
        upperSplitPane.setLeftComponent(leftSplitPane);

        tabbedPane = new JTabbedPane(SwingConstants.TOP);
        tabbedPane.setMinimumSize(new Dimension(255, 75));
        leftSplitPane.setLeftComponent(tabbedPane);

        JPanel ESPCNpanel = new JPanel();
        tabbedPane.addTab("ESPCN", null, ESPCNpanel, null);
        ESPCNpanel.setLayout(new BoxLayout(ESPCNpanel, BoxLayout.X_AXIS));

        JSeparator separator = new JSeparator();
        ESPCNpanel.add(separator);

        JRadioButton btnESPCNx2 = new JRadioButton("x2");
        btnESPCNx2.setActionCommand(DNNSuperResolutionOp.Mode.MODES[0].toString());
        mode.add(btnESPCNx2);
        btnESPCNx2.setToolTipText("x2");
        ESPCNpanel.add(btnESPCNx2);
        btnESPCNx2.setHorizontalAlignment(SwingConstants.CENTER);

        JSeparator s1 = new JSeparator();
        ESPCNpanel.add(s1);

        JRadioButton btnESPCNx3 = new JRadioButton("x3");
        btnESPCNx3.setActionCommand(DNNSuperResolutionOp.Mode.MODES[1].toString());
        mode.add(btnESPCNx3);
        btnESPCNx3.setToolTipText("x3");
        ESPCNpanel.add(btnESPCNx3);
        btnESPCNx3.setHorizontalAlignment(SwingConstants.CENTER);

        JSeparator s2 = new JSeparator();
        ESPCNpanel.add(s2);

        JRadioButton btnESPCNx4 = new JRadioButton("x4");
        btnESPCNx4.setActionCommand(DNNSuperResolutionOp.Mode.MODES[2].toString());
        mode.add(btnESPCNx4);
        btnESPCNx4.setToolTipText("x4");
        ESPCNpanel.add(btnESPCNx4);
        btnESPCNx4.setHorizontalAlignment(SwingConstants.CENTER);

        JSeparator s3 = new JSeparator();
        ESPCNpanel.add(s3);

        JPanel EDSRpanel = new JPanel();
        tabbedPane.addTab("EDSR", null, EDSRpanel, null);
        EDSRpanel.setLayout(new BoxLayout(EDSRpanel, BoxLayout.X_AXIS));

        JSeparator s4 = new JSeparator();
        EDSRpanel.add(s4);

        JRadioButton btnEDSRx2 = new JRadioButton("x2");
        btnEDSRx2.setActionCommand(DNNSuperResolutionOp.Mode.MODES[3].toString());
        mode.add(btnEDSRx2);
        btnEDSRx2.setToolTipText("x2");
        btnEDSRx2.setHorizontalAlignment(SwingConstants.CENTER);
        EDSRpanel.add(btnEDSRx2);

        JSeparator s5 = new JSeparator();
        EDSRpanel.add(s5);

        JRadioButton btnEDSRx3 = new JRadioButton("x3");
        btnEDSRx3.setActionCommand(DNNSuperResolutionOp.Mode.MODES[4].toString());
        mode.add(btnEDSRx3);
        btnEDSRx3.setToolTipText("x3");
        btnEDSRx3.setHorizontalAlignment(SwingConstants.CENTER);
        EDSRpanel.add(btnEDSRx3);

        JSeparator s6 = new JSeparator();
        EDSRpanel.add(s6);

        JRadioButton btnEDSRx4 = new JRadioButton("x4");
        btnEDSRx4.setActionCommand(DNNSuperResolutionOp.Mode.MODES[5].toString());
        mode.add(btnEDSRx4);
        btnEDSRx4.setToolTipText("x4");
        btnEDSRx4.setHorizontalAlignment(SwingConstants.CENTER);
        EDSRpanel.add(btnEDSRx4);

        JSeparator s7 = new JSeparator();
        EDSRpanel.add(s7);

        JPanel FSRCNNpanel = new JPanel();
        tabbedPane.addTab("FSRCNN", null, FSRCNNpanel, null);
        FSRCNNpanel.setLayout(new BoxLayout(FSRCNNpanel, BoxLayout.X_AXIS));

        JSeparator s8 = new JSeparator();
        FSRCNNpanel.add(s8);

        JRadioButton btnFSRCNNx2 = new JRadioButton("x2");
        btnFSRCNNx2.setActionCommand(DNNSuperResolutionOp.Mode.MODES[6].toString());
        mode.add(btnFSRCNNx2);
        btnFSRCNNx2.setToolTipText("x2");
        btnFSRCNNx2.setHorizontalAlignment(SwingConstants.CENTER);
        FSRCNNpanel.add(btnFSRCNNx2);

        JSeparator s9 = new JSeparator();
        FSRCNNpanel.add(s9);

        JRadioButton btnFSRCNNx3 = new JRadioButton("x3");
        btnFSRCNNx3.setActionCommand(DNNSuperResolutionOp.Mode.MODES[7].toString());
        mode.add(btnFSRCNNx3);
        btnFSRCNNx3.setToolTipText("x3");
        btnFSRCNNx3.setHorizontalAlignment(SwingConstants.CENTER);
        FSRCNNpanel.add(btnFSRCNNx3);

        JSeparator s10 = new JSeparator();
        FSRCNNpanel.add(s10);

        JRadioButton btnFSRCNNx4 = new JRadioButton("x4");
        btnFSRCNNx4.setActionCommand(DNNSuperResolutionOp.Mode.MODES[8].toString());
        mode.add(btnFSRCNNx4);
        btnFSRCNNx4.setVerticalAlignment(SwingConstants.TOP);
        btnFSRCNNx4.setToolTipText("x4");
        btnFSRCNNx4.setHorizontalAlignment(SwingConstants.CENTER);
        FSRCNNpanel.add(btnFSRCNNx4);

        JSeparator s11 = new JSeparator();
        FSRCNNpanel.add(s11);

        JPanel LapSRNpanel = new JPanel();
        tabbedPane.addTab("LapSRN", null, LapSRNpanel, null);
        LapSRNpanel.setLayout(new BoxLayout(LapSRNpanel, BoxLayout.X_AXIS));

        JSeparator s12 = new JSeparator();
        LapSRNpanel.add(s12);

        JRadioButton btnLapSRNx2 = new JRadioButton("x2");
        btnLapSRNx2.setActionCommand(DNNSuperResolutionOp.Mode.MODES[9].toString());
        mode.add(btnLapSRNx2);
        btnLapSRNx2.setToolTipText("x2");
        btnLapSRNx2.setHorizontalAlignment(SwingConstants.CENTER);
        LapSRNpanel.add(btnLapSRNx2);

        JSeparator s13 = new JSeparator();
        LapSRNpanel.add(s13);

        JRadioButton btnLapSRNx4 = new JRadioButton("x4");
        btnLapSRNx4.setActionCommand(DNNSuperResolutionOp.Mode.MODES[10].toString());
        mode.add(btnLapSRNx4);
        btnLapSRNx4.setToolTipText("x4");
        btnLapSRNx4.setHorizontalAlignment(SwingConstants.CENTER);
        LapSRNpanel.add(btnLapSRNx4);

        JSeparator s14 = new JSeparator();
        LapSRNpanel.add(s14);

        btnLapSRNx8 = new JRadioButton("x8");
        btnLapSRNx8.setActionCommand(DNNSuperResolutionOp.Mode.MODES[11].toString());
        mode.add(btnLapSRNx8);
        btnLapSRNx8.setVerticalAlignment(SwingConstants.TOP);
        btnLapSRNx8.setToolTipText("x8");
        btnLapSRNx8.setHorizontalAlignment(SwingConstants.CENTER);
        LapSRNpanel.add(btnLapSRNx8);

        JSeparator s15 = new JSeparator();
        LapSRNpanel.add(s15);

        JPanel startPanel = new JPanel();
        startPanel.setMinimumSize(new Dimension(70, 75));
        leftSplitPane.setRightComponent(startPanel);
        startPanel.setLayout(new BorderLayout(0, 0));
        startSVG = new FlatSVGIcon("start.svg", 36, 36, ClassLoader.getSystemClassLoader());
        startButton = new JButton("Start", startSVG);
        startButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        startButton.setHorizontalTextPosition(SwingConstants.CENTER);
        startButton.addActionListener(e -> {
            if (loadPath != null)
                createWorker().execute();
            else
                write("Load File First !", SCARLET);
        });
        startPanel.add(startButton, BorderLayout.CENTER);

        JSplitPane rightSplitPane = new JSplitPane();
        rightSplitPane.setMinimumSize(new Dimension(120, 75));
        rightSplitPane.setResizeWeight(0.5);
        upperSplitPane.setRightComponent(rightSplitPane);

        JPanel loadPanel = new JPanel();
        rightSplitPane.setLeftComponent(loadPanel);
        loadPanel.setLayout(new BorderLayout(0, 0));
        loadPanel.setMinimumSize(new Dimension(60, 75));

        loadSVG = new FlatSVGIcon("load.svg", 36, 36, ClassLoader.getSystemClassLoader());
        loadOkSVG = new FlatSVGIcon("loadOK.svg", 36, 36, ClassLoader.getSystemClassLoader());
        loadButton = new JButton("Load", loadSVG);
        loadButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        loadButton.setHorizontalTextPosition(SwingConstants.CENTER);
        loadButton.addActionListener(loadListener);

        loadPanel.add(loadButton, BorderLayout.CENTER);

        JPanel savePanel = new JPanel();
        rightSplitPane.setRightComponent(savePanel);
        savePanel.setLayout(new BorderLayout(0, 0));
        savePanel.setMinimumSize(new Dimension(60, 75));

        saveOkSVG = new FlatSVGIcon("saveOK.svg", 36, 36, ClassLoader.getSystemClassLoader());
        saveSVG = new FlatSVGIcon("save.svg", 36, 36, ClassLoader.getSystemClassLoader());
        saveButton = new JButton("Save", saveSVG);
        saveButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        saveButton.setHorizontalTextPosition(SwingConstants.CENTER);
        saveButton.addActionListener(saveListener);
        savePanel.add(saveButton, BorderLayout.CENTER);
        frame.pack();
        mainSplitPane.setDividerLocation(0.47);
    }

    public void setMode(boolean mode) {
        enableComponents(upperSplitPane, mode);
        if (mode) {
            startButton.setIcon(startSVG);
            startButton.setText("Start");
            saveButton.setIcon(saveSVG);
            loadButton.setIcon(loadSVG);
            console.addMouseListener(skinChanger);
            return;
        }
        startButton.setText(null);
        enableComponents(upperSplitPane, false);
        console.removeMouseListener(skinChanger);
        if (prefs.getBoolean("Dark Mode", true)) {
            startButton.setIcon(blackLoadingGIF);
            startButton.setDisabledIcon(blackLoadingGIF);
        } else {
            startButton.setIcon(whiteLoadingGIF);
            startButton.setDisabledIcon(whiteLoadingGIF);
        }
    }

    private Stopwatch stopwatch;

    boolean isMac() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("mac");
    }

    private ActionListener saveListener = event -> {
        // https://hub.jmonkeyengine.org/t/macos-nswindow-drag-regions-should-only-be-invalidated-on-the-main-thread/43523/2
        if (isMac()) {
            Foundation.runOnMainThread(this::save);
        } else {
            save();
        }
    };

    void save() {
        PointerBuffer path = MemoryUtil.memAllocPointer(1);
        String openPath = loadPath != null ? loadPath : prefs.get("Selected Directory", System.getProperty("user.home"));
        switch (NativeFileDialog.NFD_SaveDialog(path, null, openPath, null)) {
        case NativeFileDialog.NFD_OKAY:
            savePath = path.getStringUTF8(0);
            if (!savePath.endsWith(".png"))
                savePath += ".png";
            write("Saving to " + savePath, null);
            NativeFileDialog.NFD_FreePath(path.get(0));
            saveButton.setIcon(saveOkSVG);
            break;
        case NativeFileDialog.NFD_CANCEL:
            write("Canceled Save Location Selection", null);
            break;
        default: // NFD_ERROR
            write("Error: %s%n" + NativeFileDialog.NFD_GetError(), SCARLET);
        }
    }

    private ActionListener loadListener = event -> {
        // https://hub.jmonkeyengine.org/t/macos-nswindow-drag-regions-should-only-be-invalidated-on-the-main-thread/43523/2
        if (isMac()) {
            Foundation.runOnMainThread(this::load);
        } else {
            load();
        }
    };

    void load() {
        PointerBuffer path = MemoryUtil.memAllocPointer(1);
        String savePath = prefs.get("Selected Directory", System.getProperty("user.home"));
        switch (NativeFileDialog.NFD_OpenDialog(path, null, savePath)) {
        case NativeFileDialog.NFD_OKAY:
            loadPath = path.getStringUTF8(0);
            prefs.put("Selected Directory", new File(loadPath).getParent());
            write("Loaded " + loadPath, null);
            NativeFileDialog.NFD_FreePath(path.get(0));
            loadButton.setIcon(loadOkSVG);
            break;
        case NativeFileDialog.NFD_CANCEL:
            write("Canceled Image Selection", null);
            break;
        default: // NFD_ERROR
            write("Error: %s%n" + NativeFileDialog.NFD_GetError(), SCARLET);
        }
    }

    public SwingWorker<Boolean, Integer> createWorker() {
        return new SwingWorker<Boolean, Integer>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                setMode(false);
                stopwatch = Stopwatch.createStarted();
                DNNSuperResolutionOp DNNSuperResolutionOp = new DNNSuperResolutionOp(getMode());
                DNNSuperResolutionOp.setLogger(logger);
                try {
                    logger.fine("Loading Image: " + loadPath);
                    BufferedImage image = ImageIO.read(new File(loadPath));

                    BufferedImage filteredImage = DNNSuperResolutionOp.filter(image, null);

                    StringBuilder sb = new StringBuilder(loadPath);
                    savePath = sb.insert(sb.lastIndexOf("."), "(" + getMode() + ")").toString();
                    logger.info(savePath);

                    ImageIO.write(filteredImage, "PNG", new File(savePath));

                    prefs.put("Selected Mode", getMode().toString());
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.severe(e.getMessage());
                    return false;
                }
            }

            @Override
            protected void done() {
                boolean success = false;
                try {
                    success = get();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                stopwatch.stop();
                setMode(true);
                if (success) {
                    savePath = null;
                    printStopwatch();
                }
            }
        };
    }

    private void printStopwatch() {
        long minutes = stopwatch.elapsed(TimeUnit.MINUTES);
        long seconds = stopwatch.elapsed(TimeUnit.SECONDS);
        String minutesString = "";
        if (minutes != 0) {
            seconds = seconds % 60;
            minutesString = minutes + " ";
            if (minutes > 1)
                minutesString += "Minutes";
            else
                minutesString += "Minute";
        }
        String secondsString = "";
        if (!minutesString.equals(""))
            secondsString += ", ";
        secondsString += seconds;
        secondsString += " Second";
        if (seconds != 1)
            secondsString += "s";
        if (seconds > 10 || minutes >= 1)
            write("Done In " + minutesString + secondsString, null);
    }

    public void write(String text, Color color) {
        StyledDocument doc = console.getStyledDocument();
        Style style = console.addStyle("Color Style", null);
        if (color == null) {
            color = btnLapSRNx8.getForeground();
        } else if (color.equals(SVGBLUE))
            color = new Color(40, 164, 195);
        StyleConstants.setForeground(style, color);
        StyleConstants.setFontFamily(style, "Segoe UI");
        StyleConstants.setFontSize(style, 13);
        try {
            int length = doc.getLength();
            doc.insertString(length, " >    " + text + "\n", style);
            doc.setParagraphAttributes(length, 1, style, false);
            console.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void enableComponents(Container container, boolean enable) {
        Component[] components = container.getComponents();
        for (Component component : components) {
            component.setEnabled(enable);
            if (component instanceof Container) {
                enableComponents((Container) component, enable);
            }
        }
    }

    private DNNSuperResolutionOp.Mode getMode() {
        String thisMode = mode.getSelection().getActionCommand();
        for (DNNSuperResolutionOp.Mode mode : DNNSuperResolutionOp.Mode.MODES)
            if (thisMode.equals(mode.toString()))
                return mode;
        throw new NoSuchElementException(thisMode);
    }

    private void setSelected() {
        Enumeration<AbstractButton> buttons = mode.getElements();
        AbstractButton button;
        while (buttons.hasMoreElements()) {
            button = buttons.nextElement();
            if (button.getActionCommand().equals(prefs.get("Selected Mode", DNNSuperResolutionOp.Mode.MODES[0].toString()))) {
                button.setSelected(true);
                return;
            }
        }
    }

    private void startTabbedPanel() {
        int index;
        switch (prefs.get("Selected Mode", DNNSuperResolutionOp.Mode.MODES[0].toString()).substring(0, 2)) {
        case "ES":
            index = 0;
            break;
        case "ED":
            index = 1;
            break;
        case "FS":
            index = 2;
            break;
        default:
            index = 3;
        }
        tabbedPane.setSelectedIndex(index);
    }

    private void setSkin(boolean next) {

        if (next)
            prefs.putBoolean("Dark Mode", !prefs.getBoolean("Dark Mode", true));

//	          if (Config.FIELD03.getBoolean()) {
//                com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkHardIJTheme.install();
//                UIManager.put("TabbedPane.selectedBackground", DARK);
//            }
//            else {
//                com.formdev.flatlaf.intellijthemes.FlatCyanLightIJTheme.install();
//                UIManager.put("TabbedPane.selectedBackground", LIGHT);
//            }

        if (next)
            SwingUtilities.updateComponentTreeUI(frame);
    }
}
