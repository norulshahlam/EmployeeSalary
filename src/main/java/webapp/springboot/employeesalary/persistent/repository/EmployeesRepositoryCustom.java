package webapp.springboot.employeesalary.persistent.repository;

import webapp.springboot.employeesalary.persistent.entity.Employee;

import java.util.List;

/**
 * Methods to perform some custom CRUD operations on the employees
 * tables
 */
public interface EmployeesRepositoryCustom {

    /**
     * updates an employee
     *
     * @param employee the new values of the employee
     * @return {@literal true} if successfully updated, {@;literal false} otherwise
     */
    boolean updateEmployee(Employee employee);

    /**
     * Query the employees tables depending the given filters and
     * limits the output if required
     *
     * @param minSalary
     * @param maxSalary
     * @param limit
     * @param offset
     * @param sortBy
     * @param ascendingOrder
     * @return list of employees
     */
    List<Employee> fetchEmployee(float minSalary, float maxSalary, int limit, int offset,
                                 String sortBy, boolean ascendingOrder);

    /**
     * Checks weather the given employee is exists or not
     *
     * @param eid the employee id to check
     * @return {@literal true} is already exists, {@literal false} otherwise
     */
    boolean existsByEmployeeId(String eid);

    /**
     * Checks weather the given login already exists or not
     *
     * @param login the login to check
     * @return {@literal true} if already exists, {@literal false} otherwise
     */
    boolean existsByLogin(String login);
}
