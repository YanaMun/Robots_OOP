package gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class CoordinatesWindow extends JInternalFrame implements PropertyChangeListener {
    private final RobotsModel m_model;
    private final JLabel m_coordsLabel;

    public CoordinatesWindow(RobotsModel model) {
        super("Координаты робота", true, true, true, true);
        m_model = model;


        m_model.addPropertyChangeListener(this);

        m_coordsLabel = new JLabel("X: 0.0, Y: 0.0");
        m_coordsLabel.setFont(new java.awt.Font("Monospace", java.awt.Font.BOLD, 14));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(m_coordsLabel, BorderLayout.CENTER);
        getContentPane().add(panel);

        pack();
        setLocation(10, 100);

        updateCoords();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("coordsChanged".equals(evt.getPropertyName())) {
            EventQueue.invokeLater(this::updateCoords);
        }
    }

    private void updateCoords() {
        double x = m_model.getRobotX();
        double y = m_model.getRobotY();
        double fieldWidth = m_model.getFieldWidth();
        double fieldHeight = m_model.getFieldHeight();

        String text = String.format("X: %.1f, Y: %.1f\nПоле: %.0f x %.0f",
                x, y, fieldWidth, fieldHeight);

        if (x <= 0 || x >= fieldWidth || y <= 0 || y >= fieldHeight) {
            m_coordsLabel.setForeground(java.awt.Color.RED);
            text += "\n[ГРАНИЦА!]";
        } else {
            m_coordsLabel.setForeground(java.awt.Color.BLACK);
        }

        m_coordsLabel.setText(text);
    }
}