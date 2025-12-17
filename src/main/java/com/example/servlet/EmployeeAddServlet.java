package com.example.servlet;

import com.example.CadpClient;
import com.example.model.Employee;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

@WebServlet("/api/employees/add")
public class EmployeeAddServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        String payload = buffer.toString();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();
        
        Employee employee;
        try {
            employee = gson.fromJson(payload, Employee.class);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Invalid JSON format");
            return;
        }

        if (employee == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Empty data");
            return;
        }

        // Encrypt SSN
        String originalSsn = employee.getSsn();
        String encryptedSsn = null;
        if (originalSsn != null && !originalSsn.isEmpty()) {
            encryptedSsn = CadpClient.getInstance().enc(originalSsn);
            if (encryptedSsn == null) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                writeError(resp, "SSN Encryption failed");
                return;
            }
        }

        String url = "jdbc:mysql://mysql:3306/mysql_employees?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";
        String dbUser = "testuser";
        String dbPassword = "testpassword";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeError(resp, "JDBC Driver not found");
            return;
        }

        String sql = "INSERT INTO employee (emp_no, birth_date, first_name, last_name, gender, hire_date, ssn_no) VALUES (?, ?, ?, ?, ?, ?, ?)";
        // Note: Column names in DB are assumed based on EmployeeOriginalServlet SELECT:
        // emp_no, date_of_birth, first_name, last_name, gender, date_of_hiring, ssn_no
        // Wait, the SELECT was: emp_no, date_of_birth, first_name, last_name, gender, date_of_hiring, ssn_no
        // So INSERT columns should match those names: 
        // emp_no, date_of_birth, first_name, last_name, gender, date_of_hiring, ssn_no
        
        sql = "INSERT INTO employee (emp_no, date_of_birth, first_name, last_name, gender, date_of_hiring, ssn_no) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employee.getEmpNo());
            stmt.setObject(2, employee.getDateOfBirth()); // JDBC 4.2 supports LocalDate
            stmt.setString(3, employee.getFirstName());
            stmt.setString(4, employee.getLastName());
            stmt.setString(5, employee.getGender());
            stmt.setObject(6, employee.getDateOfHiring());
            stmt.setString(7, encryptedSsn);

            int rows = stmt.executeUpdate();
            
            PrintWriter out = resp.getWriter();
            if (rows > 0) {
                out.print("{\"message\": \"Employee added successfully\"}");
            } else {
                writeError(resp, "Failed to add employee");
            }
            out.flush();

        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeError(resp, "Database error: " + e.getMessage());
        }
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        PrintWriter out = resp.getWriter();
        out.print("{\"error\": \"" + message + "\"}");
        out.flush();
    }

    private static class LocalDateAdapter extends TypeAdapter<LocalDate> {
        @Override
        public void write(JsonWriter jsonWriter, LocalDate localDate) throws IOException {
            if (localDate == null)
                jsonWriter.nullValue();
            else
                jsonWriter.value(localDate.toString());
        }

        @Override
        public LocalDate read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == com.google.gson.stream.JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            } else {
                return LocalDate.parse(jsonReader.nextString());
            }
        }
    }
}
