package webapp.springboot.employeesalary.persistent.repository;

import org.springframework.data.repository.CrudRepository;
import webapp.springboot.employeesalary.persistent.entity.Employee;

public interface EmployeesRepository extends CrudRepository<Employee,String>, EmployeesRepositoryCustom {
}
