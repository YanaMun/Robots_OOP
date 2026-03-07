package gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
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
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import log.Logger;

public class MainApplicationFrame extends JFrame
{
    private final JDesktopPane desktopPane = new JDesktopPane();

    private static final String CONFIG_FILE_PATH =
            System.getProperty("user.home") + "/robot_app.properties";

    private LogWindow logWindow;
    private GameWindow gameWindow;

    public MainApplicationFrame() {
        int inset = 50;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(inset, inset,
                screenSize.width  - inset * 2,
                screenSize.height - inset * 2);

        setContentPane(desktopPane);

        logWindow = createLogWindow();
        addWindow(logWindow);

        gameWindow = new GameWindow();
        gameWindow.setSize(400, 400);
        addWindow(gameWindow);

        restoreWindowSettings();

        setJMenuBar(generateMenuBar());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });
    }

    protected LogWindow createLogWindow()
    {
        LogWindow logWindow = new LogWindow(Logger.getDefaultLogSource());
        logWindow.setLocation(10, 10);
        logWindow.setSize(300, 800);
        setMinimumSize(logWindow.getSize());
        logWindow.pack();
        Logger.debug("Протокол работает");
        return logWindow;
    }

    protected void addWindow(JInternalFrame frame)
    {
        desktopPane.add(frame);
        frame.setVisible(true);
    }

    private JMenuBar generateMenuBar()
    {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Файл");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem exitItem = new JMenuItem("Выход", KeyEvent.VK_X);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        exitItem.addActionListener((event) -> exitApplication());

        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        JMenu lookAndFeelMenu = new JMenu("Режим отображения");
        lookAndFeelMenu.setMnemonic(KeyEvent.VK_V);
        lookAndFeelMenu.getAccessibleContext().setAccessibleDescription(
                "Управление режимом отображения приложения");

        {
            JMenuItem systemLookAndFeel = new JMenuItem("Системная схема", KeyEvent.VK_S);
            systemLookAndFeel.addActionListener((event) -> {
                setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                this.invalidate();
            });
            lookAndFeelMenu.add(systemLookAndFeel);
        }

        {
            JMenuItem crossplatformLookAndFeel = new JMenuItem("Универсальная схема", KeyEvent.VK_U);
            crossplatformLookAndFeel.addActionListener((event) -> {
                setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                this.invalidate();
            });
            lookAndFeelMenu.add(crossplatformLookAndFeel);
        }

        JMenu testMenu = new JMenu("Тесты");
        testMenu.setMnemonic(KeyEvent.VK_T);
        testMenu.getAccessibleContext().setAccessibleDescription(
                "Тестовые команды");

        {
            JMenuItem addLogMessageItem = new JMenuItem("Сообщение в лог", KeyEvent.VK_L);
            addLogMessageItem.addActionListener((event) -> {
                Logger.debug("Новая строка");
            });
            testMenu.add(addLogMessageItem);
        }

        menuBar.add(lookAndFeelMenu);
        menuBar.add(testMenu);
        return menuBar;
    }

    private void setLookAndFeel(String className)
    {
        try
        {
            UIManager.setLookAndFeel(className);
            SwingUtilities.updateComponentTreeUI(this);
        }
        catch (ClassNotFoundException | InstantiationException
               | IllegalAccessException | UnsupportedLookAndFeelException e)
        {
            // ignore
        }
    }

    private void exitApplication()
    {
        UIManager.put("OptionPane.yesButtonText", "Да");
        UIManager.put("OptionPane.noButtonText", "Нет");

        int result = JOptionPane.showConfirmDialog(
                this,
                "Вы действительно хотите выйти?",
                "Подтверждение выхода",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION)
        {
            saveWindowSettings();
            dispose();
            System.exit(0);
        }
    }

    private void saveWindowSettings()
    {
        Properties properties = new Properties();

        saveWindowProperties(properties, "log", logWindow);
        saveWindowProperties(properties, "game", gameWindow);

        try (FileOutputStream outputStream = new FileOutputStream(CONFIG_FILE_PATH))
        {
            properties.store(outputStream, "Window settings");
        }
        catch (IOException e)
        {
            Logger.error("Не удалось сохранить настройки окон");
        }
    }

    private void restoreWindowSettings()
    {
        Properties properties = new Properties();

        try (FileInputStream inputStream = new FileInputStream(CONFIG_FILE_PATH))
        {
            properties.load(inputStream);

            restoreWindowProperties(properties, "log", logWindow);
            restoreWindowProperties(properties, "game", gameWindow);
        }
        catch (IOException e)
        {
            // если файла ещё нет, просто запускаемся с настройками по умолчанию
        }
    }

    private void saveWindowProperties(Properties properties, String prefix, JInternalFrame frame)
    {
        properties.setProperty(prefix + ".x", Integer.toString(frame.getX()));
        properties.setProperty(prefix + ".y", Integer.toString(frame.getY()));
        properties.setProperty(prefix + ".width", Integer.toString(frame.getWidth()));
        properties.setProperty(prefix + ".height", Integer.toString(frame.getHeight()));
        properties.setProperty(prefix + ".icon", Boolean.toString(frame.isIcon()));
        properties.setProperty(prefix + ".maximum", Boolean.toString(frame.isMaximum()));
    }

    private void restoreWindowProperties(Properties properties, String prefix, JInternalFrame frame)
    {
        int x = Integer.parseInt(properties.getProperty(prefix + ".x", Integer.toString(frame.getX())));
        int y = Integer.parseInt(properties.getProperty(prefix + ".y", Integer.toString(frame.getY())));
        int width = Integer.parseInt(properties.getProperty(prefix + ".width", Integer.toString(frame.getWidth())));
        int height = Integer.parseInt(properties.getProperty(prefix + ".height", Integer.toString(frame.getHeight())));
        boolean icon = Boolean.parseBoolean(properties.getProperty(prefix + ".icon", "false"));
        boolean maximum = Boolean.parseBoolean(properties.getProperty(prefix + ".maximum", "false"));

        frame.setBounds(x, y, width, height);

        try
        {
            if (maximum)
            {
                frame.setMaximum(true);
            }
            if (icon)
            {
                frame.setIcon(true);
            }
        }
        catch (PropertyVetoException e)
        {
            Logger.error("Не удалось восстановить состояние окна: " + frame.getTitle());
        }
    }
}