package webapp.springboot.employeesalary.persistent.repository;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import webapp.springboot.employeesalary.persistent.entity.Employee;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigInteger;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Implementation of {@link EmployeesRepositoryCustom}
 */
@Repository
@Transactional
public class EmployeesRepositoryImpl implements EmployeesRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public boolean updateEmployee(Employee employee) {
		Query updateQuery = entityManager
				.createNativeQuery("UPDATE employees SET login = ?, name = ?, salary = ?, startDate = ? WHERE id = ?;")
				.setParameter(1, employee.getLogin()).setParameter(2, employee.getName())
				.setParameter(3, employee.getSalary())
				.setParameter(4, employee.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
				.setParameter(5, employee.getId());
		return updateQuery.executeUpdate() > 0;
	}

	@Override
	public List<Employee> fetchEmployee(float minSalary, float maxSalary, int limit, int offset, String sortBy,
			boolean ascendingOrder) {
		String sql = "SELECT * FROM employees WHERE salary >= ? AND salary < ?";

		if (null != sortBy && !"".equals(sortBy)) {
			sql += " ORDER BY " + sortBy + " " + (ascendingOrder ? "ASC" : "DESC");
		}
		if (limit > 0) {
			sql += " LIMIT " + limit;
			if (offset > 0) {
				sql += " OFFSET " + offset;
			}
		}
		return entityManager.createNativeQuery(sql, Employee.class).setParameter(1, minSalary)
				.setParameter(2, maxSalary).getResultList();
	}

	@Override
	public boolean existsByEmployeeId(String eid) {
		Query query = entityManager.createNativeQuery("SELECT COUNT(id) FROM employees WHERE id = ?;").setParameter(1,
				eid);
		BigInteger count = (BigInteger) query.getSingleResult();
		return count.intValue() > 0;
	}

	@Override
	public boolean existsByLogin(String login) {
		Query query = entityManager.createNativeQuery("SELECT COUNT(login) FROM employees WHERE login = ?;")
				.setParameter(1, login);
		BigInteger count = (BigInteger) query.getSingleResult();
		return count.intValue() > 0;
	}
}
