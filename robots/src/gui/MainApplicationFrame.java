package gui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import log.Logger;

public class MainApplicationFrame extends JFrame {
    private final JDesktopPane desktopPane = new JDesktopPane();
    private final RobotsModel m_robotModel = new RobotsModel();

    private LogWindow m_logWindow;
    private GameWindow m_gameWindow;
    private CoordinatesWindow m_coordsWindow;

    private final File configFile;
    private final Properties properties;

    public MainApplicationFrame() {
        String home = System.getProperty("user.home");
        this.configFile = new File(home, "my_app_config.properties");
        this.properties = new Properties();

        int inset = 50;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(inset, inset,
                screenSize.width - inset * 2,
                screenSize.height - inset * 2);

        setContentPane(desktopPane);

        m_logWindow = createLogWindow();
        addWindow(m_logWindow);

        m_gameWindow = new GameWindow(m_robotModel);
        m_gameWindow.setSize(400, 400);
        m_gameWindow.putClientProperty("window_id", "game_window");
        addWindow(m_gameWindow);

        m_coordsWindow = new CoordinatesWindow(m_robotModel);
        m_coordsWindow.putClientProperty("window_id", "coords_window");
        addWindow(m_coordsWindow);

        setJMenuBar(generateMenuBar());
        loadWindowSettings();

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });
    }

    protected LogWindow createLogWindow() {
        LogWindow logWindow = new LogWindow(Logger.getDefaultLogSource());
        logWindow.setLocation(10, 10);
        logWindow.setSize(300, 800);
        logWindow.putClientProperty("window_id", "log_window");
        logWindow.pack();
        Logger.debug("Протокол работает");
        return logWindow;
    }

    protected void addWindow(JInternalFrame frame) {
        desktopPane.add(frame);
        frame.setVisible(true);

        frame.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);

        frame.addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameClosing(InternalFrameEvent e) {
                try {
                    frame.setClosed(false);
                    frame.setVisible(false);
                    Logger.debug("Окно скрыто: " + frame.getTitle());
                } catch (PropertyVetoException ex) {
                    Logger.error("Не удалось скрыть окно: " + ex.getMessage());
                }
            }
        });
    }

    private void showWindow(JInternalFrame frame, String name) {
        if (frame == null) {
            Logger.error("Окно " + name + " не инициализировано!");
            return;
        }

        if (frame.isClosed()) {
            try {
                frame.setClosed(false);
            } catch (PropertyVetoException ex) {
                Logger.error("Не удалось открыть окно " + name);
                return;
            }
        }

        frame.setVisible(true);
        try {
            frame.setIcon(false);
            frame.setMaximum(false);
        } catch (PropertyVetoException ex) {
            return;
        }
        frame.toFront();
        Logger.debug("Окно открыто: " + name);
    }

    private void showLogWindow() {
        showWindow(m_logWindow, "Протокол");
    }

    private void showGameWindow() {
        showWindow(m_gameWindow, "Игровое поле");
    }

    private void showCoordsWindow() {
        showWindow(m_coordsWindow, "Координаты");
    }

    private void loadWindowSettings() {
        if (!configFile.exists()) {
            Logger.debug("Файл конфигурации не найден");
            return;
        }
        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
        } catch (IOException e) {
            Logger.error("Ошибка загрузки настроек: " + e.getMessage());
            return;
        }
        for (JInternalFrame frame : desktopPane.getAllFrames()) {
            String id = (String) frame.getClientProperty("window_id");
            if (id != null) {
                restoreInternalFrame(frame, id);
            }
        }
    }

    private void saveWindowSettings() {
        for (JInternalFrame frame : desktopPane.getAllFrames()) {
            String id = (String) frame.getClientProperty("window_id");
            if (id != null) {
                saveInternalFrame(frame, id);
            }
        }
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            properties.store(fos, "Internal Frames Configuration");
        } catch (IOException e) {
            Logger.error("Ошибка сохранения настроек: " + e.getMessage());
        }
    }

    private void saveInternalFrame(JInternalFrame frame, String id) {
        if (frame == null) return;
        properties.setProperty(id + ".x", String.valueOf(frame.getX()));
        properties.setProperty(id + ".y", String.valueOf(frame.getY()));
        properties.setProperty(id + ".width", String.valueOf(frame.getWidth()));
        properties.setProperty(id + ".height", String.valueOf(frame.getHeight()));
        properties.setProperty(id + ".is_maximum", String.valueOf(frame.isMaximum()));
        properties.setProperty(id + ".is_icon", String.valueOf(frame.isIcon()));
    }

    private void restoreInternalFrame(JInternalFrame frame, String id) {
        if (frame == null) return;
        int x = Integer.parseInt(properties.getProperty(id + ".x", "10"));
        int y = Integer.parseInt(properties.getProperty(id + ".y", "10"));
        int w = Integer.parseInt(properties.getProperty(id + ".width", "400"));
        int h = Integer.parseInt(properties.getProperty(id + ".height", "300"));
        boolean isMaximum = Boolean.parseBoolean(properties.getProperty(id + ".is_maximum", "false"));
        boolean isIcon = Boolean.parseBoolean(properties.getProperty(id + ".is_icon", "false"));

        try {
            if (frame.isMaximum()) frame.setMaximum(false);
            if (frame.isIcon()) frame.setIcon(false);
            frame.setBounds(x, y, w, h);
            if (isMaximum) frame.setMaximum(true);
            if (isIcon) frame.setIcon(true);
        } catch (PropertyVetoException ignored) {
        }
    }

    private JMenuBar generateMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createLookAndFeelMenu());
        menuBar.add(createTestMenu());
        menuBar.add(createWindowMenu());
        return menuBar;
    }

    private JMenu createFileMenu() {
        JMenu fileMenu = new JMenu("Файл");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenuItem exitItem = new JMenuItem("Выход", KeyEvent.VK_X);
        exitItem.addActionListener(e -> exitApplication());
        fileMenu.add(exitItem);
        return fileMenu;
    }

    private JMenu createLookAndFeelMenu() {
        JMenu lookAndFeelMenu = new JMenu("Режим отображения");
        lookAndFeelMenu.setMnemonic(KeyEvent.VK_V);
        JMenuItem systemLookAndFeel = new JMenuItem("Системная схема", KeyEvent.VK_S);
        systemLookAndFeel.addActionListener((event) -> {
            setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            this.invalidate();
        });
        lookAndFeelMenu.add(systemLookAndFeel);
        JMenuItem crossplatformLookAndFeel = new JMenuItem("Универсальная схема", KeyEvent.VK_U);
        crossplatformLookAndFeel.addActionListener((event) -> {
            setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            this.invalidate();
        });
        lookAndFeelMenu.add(crossplatformLookAndFeel);
        return lookAndFeelMenu;
    }

    private JMenu createTestMenu() {
        JMenu testMenu = new JMenu("Тесты");
        testMenu.setMnemonic(KeyEvent.VK_T);
        JMenuItem addLogMessageItem = new JMenuItem("Сообщение в лог", KeyEvent.VK_S);
        addLogMessageItem.addActionListener((event) -> Logger.debug("Новая строка"));
        testMenu.add(addLogMessageItem);
        return testMenu;
    }

    private JMenu createWindowMenu() {
        JMenu windowMenu = new JMenu("Окна");
        windowMenu.setMnemonic(KeyEvent.VK_O);

        JMenuItem showLogItem = new JMenuItem("Протокол работы", KeyEvent.VK_P);
        showLogItem.addActionListener(e -> showLogWindow());
        windowMenu.add(showLogItem);

        JMenuItem showGameItem = new JMenuItem("Игровое поле", KeyEvent.VK_I);
        showGameItem.addActionListener(e -> showGameWindow());
        windowMenu.add(showGameItem);

        JMenuItem showCoordsItem = new JMenuItem("Координаты робота", KeyEvent.VK_C);
        showCoordsItem.addActionListener(e -> showCoordsWindow());
        windowMenu.add(showCoordsItem);

        return windowMenu;
    }

    private void exitApplication() {
        Object[] options = {"Да", "Нет"};
        int result = JOptionPane.showOptionDialog(
                this,
                "Вы действительно хотите выйти из приложения?",
                "Подтверждение выхода",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]
        );
        if (result == JOptionPane.YES_OPTION) {
            saveWindowSettings();
            Logger.debug("Приложение завершает работу");
            System.exit(0);
        }
    }

    private void setLookAndFeel(String className) {
        try {
            UIManager.setLookAndFeel(className);
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}