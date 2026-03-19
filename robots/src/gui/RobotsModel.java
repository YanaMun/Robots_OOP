package gui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.awt.Point;

public class RobotsModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    // Координаты робота
    private volatile double m_robotPositionX = 100;
    private volatile double m_robotPositionY = 100;
    private volatile double m_robotDirection = 0;

    // Цель
    private volatile double m_targetPositionX = 150;
    private volatile double m_targetPositionY = 100;

    // Скорость и угловая скорость
    private static final double maxVelocity = 0.1;
    private static final double maxAngularVelocity = 0.001;

    // Границы поля
    private volatile double fieldWidth = 800;
    private volatile double fieldHeight = 600;

    public RobotsModel() {
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    public double getRobotX() {
        return m_robotPositionX;
    }

    public double getRobotY() {
        return m_robotPositionY;
    }

    public double getRobotDirection() {
        return m_robotDirection;
    }

    public double getTargetX() {
        return m_targetPositionX;
    }

    public double getTargetY() {
        return m_targetPositionY;
    }

    public double getFieldWidth() {
        return fieldWidth;
    }

    public double getFieldHeight() {
        return fieldHeight;
    }


    public void setTargetPosition(Point p) {
        m_targetPositionX = p.x;
        m_targetPositionY = p.y;
        support.firePropertyChange("targetChanged", null, p);
    }

    public void setFieldBounds(double width, double height) {
        boolean changed = (this.fieldWidth != width || this.fieldHeight != height);
        this.fieldWidth = width;
        this.fieldHeight = height;

        if (changed) {
            applyFieldBounds();
            support.firePropertyChange("coordsChanged", null, null);
        }
    }


    public void updateModel() {
        double distance = distance(m_targetPositionX, m_targetPositionY,
                m_robotPositionX, m_robotPositionY);

        if (distance < 0.5) {
            return;
        }

        double velocity = maxVelocity;
        double angleToTarget = angleTo(m_robotPositionX, m_robotPositionY,
                m_targetPositionX, m_targetPositionY);


        double angleDiff = angleToTarget - m_robotDirection;
        while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
        while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

        double angularVelocity = 0;
        if (angleDiff > 0.01) {
            angularVelocity = maxAngularVelocity;
        } else if (angleDiff < -0.01) {
            angularVelocity = -maxAngularVelocity;
        }

        moveRobot(velocity, angularVelocity, 10);
        applyFieldBounds();

        support.firePropertyChange("coordsChanged", null, null);
    }

    private void applyFieldBounds() {
        if (m_robotPositionX < 0) m_robotPositionX = 0;
        if (m_robotPositionX > fieldWidth) m_robotPositionX = fieldWidth;
        if (m_robotPositionY < 0) m_robotPositionY = 0;
        if (m_robotPositionY > fieldHeight) m_robotPositionY = fieldHeight;
    }

    private void moveRobot(double velocity, double angularVelocity, double duration) {
        velocity = applyLimits(velocity, 0, maxVelocity);
        angularVelocity = applyLimits(angularVelocity, -maxAngularVelocity, maxAngularVelocity);

        double newX, newY;
        if (Math.abs(angularVelocity) < 0.0001) {
            newX = m_robotPositionX + velocity * duration * Math.cos(m_robotDirection);
            newY = m_robotPositionY + velocity * duration * Math.sin(m_robotDirection);
        } else {
            newX = m_robotPositionX + velocity / angularVelocity *
                    (Math.sin(m_robotDirection + angularVelocity * duration) - Math.sin(m_robotDirection));
            newY = m_robotPositionY - velocity / angularVelocity *
                    (Math.cos(m_robotDirection + angularVelocity * duration) - Math.cos(m_robotDirection));
        }

        if (!Double.isFinite(newX) || !Double.isFinite(newY)) {
            newX = m_robotPositionX + velocity * duration * Math.cos(m_robotDirection);
            newY = m_robotPositionY + velocity * duration * Math.sin(m_robotDirection);
        }

        m_robotPositionX = newX;
        m_robotPositionY = newY;
        m_robotDirection = asNormalizedRadians(m_robotDirection + angularVelocity * duration);
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        double diffX = x1 - x2;
        double diffY = y1 - y2;
        return Math.sqrt(diffX * diffX + diffY * diffY);
    }

    private static double angleTo(double fromX, double fromY, double toX, double toY) {
        double diffX = toX - fromX;
        double diffY = toY - fromY;
        return asNormalizedRadians(Math.atan2(diffY, diffX));
    }

    private static double applyLimits(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private static double asNormalizedRadians(double angle) {
        while (angle < 0) angle += 2 * Math.PI;
        while (angle >= 2 * Math.PI) angle -= 2 * Math.PI;
        return angle;
    }
}