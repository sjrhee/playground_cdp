package com.example.model;

import java.time.LocalDate;

public class Employee {
    private int empNo;
    private LocalDate dateOfBirth;
    private String firstName;
    private String lastName;
    private String gender; // ENUM('M', 'F')
    private LocalDate dateOfHiring;
    private String ssn;

    public Employee(int empNo, LocalDate dateOfBirth, String firstName, String lastName, String gender, LocalDate dateOfHiring, String ssn) {
        this.empNo = empNo;
        this.dateOfBirth = dateOfBirth;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.dateOfHiring = dateOfHiring;
        this.ssn = ssn;
    }

    // Getters
    public int getEmpNo() { return empNo; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getGender() { return gender; }
    public LocalDate getDateOfHiring() { return dateOfHiring; }
    public String getSsn() { return ssn; }
}
