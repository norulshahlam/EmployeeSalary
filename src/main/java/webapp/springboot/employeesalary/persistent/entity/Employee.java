package webapp.springboot.employeesalary.persistent.entity;

import javax.persistence.*;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;

/**
 * Employee class contains the details a single row
 * in employees table in the database.
 * The "employees" table is created by executing the
 * "employees_db.sql" script in /src/main/resources
 */
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @Column
    private String id; // primary key of the table
    @Column
    private String login; // login value is unique in table
    @Column
    private String name; // non unique full name of the employee
    @Column
    private float salary; // always >= 0

    // valid date formats are
    // dd-MMM-yy: example -> 16-Nov-01
    // yyyy-MM-dd: eample -> 2001-11-16
    @Column(name = "startdate")
    private LocalDate startDate;

    public Employee() {}

    public Employee(String id, String login, String name, float salary, LocalDate startDate) {
        this.id = id;
        this.login = login;
        this.name = inUTF8String(name);
        this.salary = salary;
        this.startDate = startDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = inUTF8String(name);
    }

    public float getSalary() {
        return salary;
    }

    public void setSalary(float salary) {
        this.salary = salary;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * encodes a string in UTF-8
     *
     * @param s input string
     * @return the UTF-8 encoded string
     */
    private String inUTF8String(String s) {
        try {
            return new String(s.getBytes("ISO-8859-1"),"UTF-8");
        } catch (UnsupportedEncodingException e) {}
        return s;
    }
}
