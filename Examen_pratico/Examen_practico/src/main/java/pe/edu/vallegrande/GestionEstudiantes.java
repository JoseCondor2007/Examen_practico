package pe.edu.vallegrande;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GestionEstudiantes extends JFrame implements ActionListener {

    private JTextField nombreField, apellidoField, correoField;
    private JButton insertarButton, modificarButton, eliminarLogicoButton, listarButton, mostrarEliminadosButton;
    private JTable estudiantesTable;
    private DefaultTableModel tableModel;

    public GestionEstudiantes() {
        setTitle("Gestión de Estudiantes");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLayout(new BorderLayout());

        // Panel para los campos de entrada y botones
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        JLabel nombreLabel = new JLabel("Nombre:");
        nombreField = new JTextField();
        JLabel apellidoLabel = new JLabel("Apellido:");
        apellidoField = new JTextField();
        JLabel correoLabel = new JLabel("Correo:");
        correoField = new JTextField();

        insertarButton = new JButton("Insertar");
        modificarButton = new JButton("Modificar");
        eliminarLogicoButton = new JButton("Eliminar Lógico");
        listarButton = new JButton("Listar Activos");
        mostrarEliminadosButton = new JButton("Mostrar Eliminados");

        inputPanel.add(nombreLabel);
        inputPanel.add(nombreField);
        inputPanel.add(apellidoLabel);
        inputPanel.add(apellidoField);
        inputPanel.add(correoLabel);
        inputPanel.add(correoField);
        inputPanel.add(insertarButton);
        inputPanel.add(modificarButton);
        inputPanel.add(eliminarLogicoButton);
        inputPanel.add(listarButton);
        inputPanel.add(new JLabel()); // Espacio
        inputPanel.add(mostrarEliminadosButton);

        add(inputPanel, BorderLayout.NORTH);

        // Tabla para mostrar los estudiantes
        tableModel = new DefaultTableModel(new Object[]{"ID", "Nombre", "Apellido", "Correo", "Estado"}, 0);
        estudiantesTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(estudiantesTable);
        add(scrollPane, BorderLayout.CENTER);

        // Asignar ActionListener a los botones
        insertarButton.addActionListener(this);
        modificarButton.addActionListener(this);
        eliminarLogicoButton.addActionListener(this);
        listarButton.addActionListener(this);
        mostrarEliminadosButton.addActionListener(this);

        // Listener para la selección de la tabla al modificar
        estudiantesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = estudiantesTable.getSelectedRow();
                if (selectedRow != -1) {
                    nombreField.setText(tableModel.getValueAt(selectedRow, 1).toString());
                    apellidoField.setText(tableModel.getValueAt(selectedRow, 2).toString());
                    correoField.setText(tableModel.getValueAt(selectedRow, 3).toString());
                }
            }
        });

        listarEstudiantesActivos(); // Mostrar los estudiantes activos al iniciar la aplicación

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GestionEstudiantes::new);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == insertarButton) {
            insertarEstudiante();
        } else if (e.getSource() == modificarButton) {
            modificarEstudiante();
        } else if (e.getSource() == eliminarLogicoButton) {
            eliminarEstudianteLogico();
        } else if (e.getSource() == listarButton) {
            listarEstudiantesActivos();
        } else if (e.getSource() == mostrarEliminadosButton) {
            listarEstudiantesEliminados();
        }
    }

    private void insertarEstudiante() {
        String nombre = nombreField.getText();
        String apellido = apellidoField.getText();
        String correo = correoField.getText();

        if (!nombre.isEmpty() && !apellido.isEmpty() && !correo.isEmpty()) {
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(
                         "INSERT INTO estudiantes (nombre, apellido, correo, estado) VALUES (?, ?, ?, ?)")) {
                preparedStatement.setString(1, nombre);
                preparedStatement.setString(2, apellido);
                preparedStatement.setString(3, correo);
                preparedStatement.setBoolean(4, true); // Estado activo por defecto
                int filasAfectadas = preparedStatement.executeUpdate();

                if (filasAfectadas > 0) {
                    JOptionPane.showMessageDialog(this, "Estudiante insertado correctamente.");
                    limpiarCampos();
                    listarEstudiantesActivos(); // Actualizar la tabla
                } else {
                    JOptionPane.showMessageDialog(this, "Error al insertar el estudiante.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error de conexión a la base de datos.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, complete todos los campos.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void listarEstudiantesActivos() {
        tableModel.setRowCount(0); // Limpiar la tabla
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT id, nombre, apellido, correo, estado FROM estudiantes WHERE estado = ?")) {
            preparedStatement.setBoolean(1, true);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String nombre = resultSet.getString("nombre");
                    String apellido = resultSet.getString("apellido");
                    String correo = resultSet.getString("correo");
                    boolean estado = resultSet.getBoolean("estado");
                    tableModel.addRow(new Object[]{id, nombre, apellido, correo, estado});
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al listar los estudiantes activos.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modificarEstudiante() {
        int selectedRow = estudiantesTable.getSelectedRow();
        if (selectedRow != -1) {
            int idEstudiante = (int) tableModel.getValueAt(selectedRow, 0);
            String nombre = nombreField.getText();
            String apellido = apellidoField.getText();
            String correo = correoField.getText();

            if (!nombre.isEmpty() && !apellido.isEmpty() && !correo.isEmpty()) {
                try (Connection connection = DatabaseConnection.getConnection();
                     PreparedStatement preparedStatement = connection.prepareStatement(
                             "UPDATE estudiantes SET nombre = ?, apellido = ?, correo = ? WHERE id = ?")) {
                    preparedStatement.setString(1, nombre);
                    preparedStatement.setString(2, apellido);
                    preparedStatement.setString(3, correo);
                    preparedStatement.setInt(4, idEstudiante);
                    int filasAfectadas = preparedStatement.executeUpdate();

                    if (filasAfectadas > 0) {
                        JOptionPane.showMessageDialog(this, "Estudiante modificado correctamente.");
                        listarEstudiantesActivos(); // Actualizar la tabla
                        limpiarCampos();
                    } else {
                        JOptionPane.showMessageDialog(this, "No se pudo modificar el estudiante.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error de conexión a la base de datos.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Por favor, complete todos los campos.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un estudiante de la tabla para modificar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void eliminarEstudianteLogico() {
        int selectedRow = estudiantesTable.getSelectedRow();
        if (selectedRow != -1) {
            int idEstudiante = (int) tableModel.getValueAt(selectedRow, 0);

            int confirmacion = JOptionPane.showConfirmDialog(this, "¿Seguro que desea eliminar lógicamente este estudiante?", "Confirmar Eliminación", JOptionPane.YES_NO_OPTION);
            if (confirmacion == JOptionPane.YES_OPTION) {
                try (Connection connection = DatabaseConnection.getConnection();
                     PreparedStatement preparedStatement = connection.prepareStatement(
                             "UPDATE estudiantes SET estado = ? WHERE id = ?")) {
                    preparedStatement.setBoolean(1, false);
                    preparedStatement.setInt(2, idEstudiante);
                    int filasAfectadas = preparedStatement.executeUpdate();

                    if (filasAfectadas > 0) {
                        JOptionPane.showMessageDialog(this, "Estudiante eliminado lógicamente.");
                        listarEstudiantesActivos(); // Actualizar la tabla
                        limpiarCampos();
                    } else {
                        JOptionPane.showMessageDialog(this, "No se pudo eliminar el estudiante.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error de conexión a la base de datos.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un estudiante de la tabla para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void listarEstudiantesEliminados() {
        tableModel.setRowCount(0); // Limpiar la tabla
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT id, nombre, apellido, correo, estado FROM estudiantes WHERE estado = ?")) {
            preparedStatement.setBoolean(1, false);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String nombre = resultSet.getString("nombre");
                    String apellido = resultSet.getString("apellido");
                    String correo = resultSet.getString("correo");
                    boolean estado = resultSet.getBoolean("estado");
                    tableModel.addRow(new Object[]{id, nombre, apellido, correo, estado});
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al listar los estudiantes eliminados.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarCampos() {
        nombreField.setText("");
        apellidoField.setText("");
        correoField.setText("");
    }
}