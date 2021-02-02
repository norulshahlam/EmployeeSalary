package webapp.springboot.employeesalary.controller;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import webapp.springboot.employeesalary.persistent.entity.Employee;
import webapp.springboot.employeesalary.persistent.repository.EmployeesRepository;
import java.time.LocalDate;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(EmployeeSalaryController.class)
class EmployeeSalaryControllerTest {

    @MockBean
    private EmployeesRepository repository;

    @Autowired
    private MockMvc mvc;

    @Test
    void uploadEmployeeDataCSV() throws Exception {
        String content = "id,login,name,salary,startDate\n" +
                "e0005,voldemort,Lord Voldemort,523.4,17-Nov-01\n" +
                "# this is a line to ignore";
        MockMultipartFile file = new MockMultipartFile("file",
                "employees.csv",
                "text/csv",
                content.getBytes("UTF-8"));

        mvc.perform(multipart("/users/upload").file(file))
                .andExpect(status().is(201));
    }

    @Test
    void getUserById() throws Exception {
        final Employee employee = new Employee("e0001","hpotter","Harry Potter",
                1400, LocalDate.of(2001,11,16));
        when(repository.findById("e0001")).thenReturn(Optional.of(employee));
        mvc.perform(get("/users/e0001")
                .accept(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(employee.getId())))
        .andExpect(jsonPath("$.login", is(employee.getLogin())))
        .andExpect(jsonPath("$.*", hasSize(5)));
    }

    @Test
    void deleteUserById() throws Exception {
        when(repository.existsByEmployeeId("e0001")).thenReturn(true);
        mvc.perform(delete("/users/e0001"));
        verify(repository,times(1)).deleteById("e0001");
    }
}