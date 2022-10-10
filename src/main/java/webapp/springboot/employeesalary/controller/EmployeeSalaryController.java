package webapp.springboot.employeesalary.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import webapp.springboot.employeesalary.persistent.entity.Employee;
import webapp.springboot.employeesalary.persistent.repository.EmployeesRepository;

import javax.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

/**
 * Controller for all REST request for http://<web address?/users.
 * This controller handles:
 * <ol>
 *     <li>CSV Upload</li>
 *     <li>Create New Employee</li>
 *     <li>Update Existing Employee</li>
 *     <li>Delete Existing Employee</li>
 *     <li>Get An Existing Employee</li>
 *     <li>Get List Of Existing Employee By Some Criteria</li>
 * </ol>
 */


@RestController
@RequestMapping("/users")
public class EmployeeSalaryController {

    private static final String[] SORT_COLUMNS = {"name","salary","startDate"};

    private static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DD_MMM_YY = DateTimeFormatter.ofPattern("dd-MMM-yy");
    private static final DateTimeFormatter[] VALID_DATE_FORMATS = {YYYY_MM_DD,DD_MMM_YY};

    @Autowired
    private EmployeesRepository repository; // repository to handle CRUD operations

    /**
     * Handles an upload CSV request. In the following cases writing the new employee data
     * into the database will be discarded are:
     * <ul>
     *     <li>if csv contains multiple entries with same employee id</li>
     *     <li>if csv contains multiple entries with sample login</li>
     *     <li>if csv contains any empty cell value</li>
     *     <li>if login is not unique in database</li>
     * </ul>
     * Each entries in csv are inserted into database if the employee id does not exists in
     * database already, otherwise the entry is updated with the new values in the database.
     * It returns status code 200 only if the upload is successfull by no data is written to
     * the database. status code 201 is returned if upload is successful as well as new data
     * is either created or updated into the database. Otherwise 400 is returned if any error
     * occurred.
     *
     * @param csvFile the uploaded csv file
     * @return http response
     */
    @PostMapping(value = "/upload")
    public ResponseEntity<String> uploadEmployeeDataCSV(@RequestParam("file") MultipartFile csvFile) {
        List<Employee> employees = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(csvFile.getInputStream()));
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) continue;
                String[] parts = line.split(",");
                try {
                    checkEmployeeInputOrThrow(parts[1],parts[3],parts[4]);
                }
                catch (RuntimeException e) {
                    return createSimpleJSONResponse(BAD_REQUEST,e.getMessage());
                }
                employees.add(new Employee(parts[0],parts[1],parts[2],Float.parseFloat(parts[3]),parseDate(parts[4])));
            }
            if (employees.isEmpty()) {
                return createSimpleJSONResponse(OK,"Successfully uploaded but file is empty");
            }
            for (int i = 0; i < employees.size()-1; i++) {
                for (int j = i+1; j < employees.size(); j++) {
                    Employee l = employees.get(i);
                    Employee r = employees.get(j);
                    if (l.getId().equals(r.getId())) {
                        return createSimpleJSONResponse(BAD_REQUEST, "Duplicate entry found with id '"+l.getId()+"'");
                    }
                    else if (l.getLogin().equals(r.getLogin())) {
                        return createSimpleJSONResponse(BAD_REQUEST, "Duplicate login found for employees with id '"+
                                l.getId()+"' and '"+r.getId()+"'");
                    }
                    else if (repository.existsByLogin(l.getLogin())) {
                        return createSimpleJSONResponse(BAD_REQUEST, "Login is not unique");
                    }
                }
            }
        }
        catch (IOException e) {
            return createSimpleJSONResponse(BAD_REQUEST,"Error in parsing input CSV file");
        }
        employees.forEach(e -> {
            if (repository.existsByEmployeeId(e.getId())) {
                repository.updateEmployee(e);
            }
            else {
                repository.save(e);
            }
        });
        return createSimpleJSONResponse(CREATED, "Successfully uploaded and data created");
    }

    @GetMapping(value = {"","/"})
    public ResponseEntity<String> fetchListOfEmployees(@RequestParam(value = "minSalary", defaultValue = "0") float minSalary, @RequestParam(value = "maxSalary", defaultValue = "4000.00") float maxSalary,
                                                       @RequestParam(value = "offset",defaultValue = "0") int offset, @RequestParam(value = "limit",defaultValue = "0") int limit,
                                                       @RequestParam(value = "sortBy", defaultValue = "") String sortBy, @RequestParam(value = "asc",defaultValue = "1") int ascending) {
        if (minSalary < 0) {
            return createSimpleJSONResponse(BAD_REQUEST, "minSalary must be >= 0, found "+minSalary);
        }
        else if (maxSalary < minSalary) {
            return createSimpleJSONResponse(BAD_REQUEST,"maxSalary must be >= "+minSalary+", found "+maxSalary);
        }
        else if (limit < 0) {
            return createSimpleJSONResponse(BAD_REQUEST, "limit must be >= 0, found "+limit);
        }
        else if (offset < 0) {
            return createSimpleJSONResponse(BAD_REQUEST,"offset must be >= 0, found "+offset);
        }
        else if (!"".equals(sortBy) && Arrays.binarySearch(SORT_COLUMNS,sortBy) < 0) {
            return createSimpleJSONResponse(BAD_REQUEST,"Currently sort by '"+sortBy+"' is not available");
        }

        List<Employee> employees = repository.fetchEmployee(minSalary, maxSalary, offset, limit,sortBy,ascending==1);
        return createJSONResponse(OK,employeeListToJSON(employees));
    }

    /**
     * Get a single employee with the employee id given.
     * It returns the employee in json with status code 200
     * if found, otherwise status code 400 is returned with proper
     * error message
     *
     * @param id the employee id the search
     * @return http response
     */
    @GetMapping("/{id}")
    public ResponseEntity<String> getUserById(@PathVariable("id") String id) {
        Optional<Employee> employee = repository.findById(id);
        return employee.map(value -> createJSONResponse(OK, employeeToJSON(value))).orElseGet(
                () -> createSimpleJSONResponse(BAD_REQUEST, "No such employee"));
    }

    /**
     * Creates a new employee. Returns status code 201 if the new employee is
     * created successfully, otherwise status code 400 is returned is any error
     * occurred.
     *
     * @param request http POST request
     * @return http response
     */
    @PostMapping(value = {"","/"}, consumes = {MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<String> createEmployee(HttpServletRequest request) {
        final String _id = request.getParameter("id");
        if (repository.existsByEmployeeId(_id)) {
            return createSimpleJSONResponse(BAD_REQUEST,"Employee ID already exists");
        }
        try {
            checkEmployeeInputOrThrow(request);
        }
        catch (RuntimeException e) {
            return createSimpleJSONResponse(BAD_REQUEST,e.getMessage());
        }
        repository.save(new Employee(_id,
                request.getParameter("login"),
                request.getParameter("name"),
                Float.parseFloat(request.getParameter("salary")),
                parseDate(request.getParameter("startDate"))));
        return createSimpleJSONResponse(CREATED,"Successfully created");
    }

    /**
     * Updates an existing employee matched by given employee id. Status code 200 is
     * returned is the employee exists and successfuly updated, status code 400 is returned
     * if any error occurred.
     *
     * @param _id employee id to update
     * @param request http PUT or PATCH request
     * @return http response
     */
    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT,RequestMethod.PATCH})
    public ResponseEntity<String> updateUserById(@PathVariable("id") String _id, HttpServletRequest request) {
        if (!repository.existsByEmployeeId(_id)) {
            return createSimpleJSONResponse(BAD_REQUEST,"No such employee");
        }
        try {
            checkEmployeeInputOrThrow(request);
        }
        catch (RuntimeException e) {
            return createSimpleJSONResponse(BAD_REQUEST,e.getMessage());
        }
        repository.updateEmployee(new Employee(_id,
                request.getParameter("login"),
                request.getParameter("name"),
                Float.parseFloat(request.getParameter("salary")),
                parseDate(request.getParameter("startDate"))));
        return createSimpleJSONResponse(CREATED, "Successfully updated");
    }

    /**
     * Deletes an employee if exists. Status code 200 is returned if the employee with id exists and
     * deleted successfully, status code 400 is returned otherwise.
     *
     * @param id the id of the employee to delete
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUserById(@PathVariable("id") String id) {
        if (!repository.existsByEmployeeId(id)) {
            return createSimpleJSONResponse(BAD_REQUEST, "No such employee");
        }
        repository.deleteById(id);
        return createSimpleJSONResponse(OK, "Successfully deleted");
    }

    private String employeeToJSON(Employee e) {
        return "{\"id\":\""+e.getId()+"\"," +
                "\"name\":\""+e.getName()+"\"," +
                "\"login\":\""+e.getLogin()+"\"," +
                "\"salary\":"+e.getSalary()+"," +
                "\"startDate\":\""+e.getStartDate().format(YYYY_MM_DD)+"\"}";
    }

    private String employeeListToJSON(List<Employee> employees) {
        StringBuffer buffer = new StringBuffer();
        employees.forEach(e -> buffer.append(employeeToJSON(e)).append(",\n"));
        return "{\"results\":["+ buffer.toString() +"]}";
    }

    private ResponseEntity<String> createSimpleJSONResponse(HttpStatus status, String message) {
        return createJSONResponse(status, "{\"message\":\""+message+"\"}");
    }

    private ResponseEntity<String> createJSONResponse(HttpStatus status, String body) {
        return ResponseEntity.status(status).contentType(APPLICATION_JSON).body(body);
    }

    private void checkEmployeeInputOrThrow(HttpServletRequest request) throws RuntimeException {
        checkEmployeeInputOrThrow(request.getParameter("login"),
                request.getParameter("salary"),
                request.getParameter("startDate"));
    }

    private void checkEmployeeInputOrThrow(String _login, String _salary, String _startDate) throws RuntimeException {
        if (repository.existsByLogin(_login)) {
            throw new RuntimeException( "Employee login not unique");
        }
        try {
            if (Float.parseFloat(_salary) < 0) {
                throw new RuntimeException("Invalid salary");
            }
        }
        catch (NumberFormatException|NullPointerException e) {
            throw new RuntimeException("Invalid salary");
        }
        if (null == parseDate(_startDate)) {
            throw new RuntimeException("Invalid date");
        }
    }

    private LocalDate parseDate(String date) {
        for (DateTimeFormatter f : VALID_DATE_FORMATS) {
            try {
                return LocalDate.parse(date,f);
            } catch (DateTimeParseException | NullPointerException e) {}
        }
        return null;
    }
}
