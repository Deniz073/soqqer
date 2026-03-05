package nl.quintor.soqqer.employee.gateway.api;

import nl.quintor.soqqer.config.TestcontainersConfiguration;
import nl.quintor.soqqer.employee.gateway.api.dto.CreateEmployeeDTO;
import nl.quintor.soqqer.employee.gateway.api.dto.EmployeeDTO;
import nl.quintor.soqqer.employee.gateway.api.dto.UpdateEmployeeDTO;
import nl.quintor.soqqer.employee.persistence.entity.Office;
import nl.quintor.soqqer.employee.persistence.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;


@ApplicationModuleTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@Import(TestcontainersConfiguration.class)
class EmployeeControllerITTest {

    @LocalServerPort
    private int port;

    @Autowired
    private EmployeeRepository employeeRepository;

    private RestTestClient restTestClient;

    @BeforeEach
    void beforeEach() {
        employeeRepository.deleteAll();
        restTestClient = RestTestClient.bindToServer().baseUrl("http://localhost:" + port + "/api/employees").build();
    }

    @Test
    void getAllEmployees_Returns_Empty_When_No_Employees_Exist() {
        restTestClient.get().exchange()
                .expectStatus().isOk()
                .expectBody().json("[]");
    }

    @Test
    void getEmployeeById_Returns_NotFound_When_Employee_Does_Not_Exist() {
        restTestClient.get().uri("/1").exchange().expectStatus().isNotFound();
    }

    @Test
    void employee_Endpoint_Flow_Create_GetAll_GetById_Update_Delete() {
        var created = createEmployee("Deniz", Office.DENBOSCH);

        restTestClient.get().exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].id").isEqualTo(created.id())
                .jsonPath("$[0].name").isEqualTo("Deniz")
                .jsonPath("$[0].office").isEqualTo("DENBOSCH");

        restTestClient.get().uri("/{id}", created.id()).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(created.id())
                .jsonPath("$.name").isEqualTo("Deniz")
                .jsonPath("$.office").isEqualTo("DENBOSCH");

        restTestClient.put().uri("/{id}", created.id())
                .body(new UpdateEmployeeDTO("Sasha", Office.DENHAAG))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(created.id())
                .jsonPath("$.name").isEqualTo("Sasha")
                .jsonPath("$.office").isEqualTo("DENHAAG");

        restTestClient.delete().uri("/{id}", created.id()).exchange()
                .expectStatus().isNoContent();

        restTestClient.get().uri("/{id}", created.id()).exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Entity Not Found")
                .jsonPath("$.detail").isEqualTo("Entity not found");
    }

    @Test
    void createEmployee_Returns_Conflict_When_Duplicate_Exists() {
        createEmployee("Robin", Office.DEVENTER);

        restTestClient.post()
                .body(new CreateEmployeeDTO("Robin", Office.DEVENTER))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Employee already exists")
                .jsonPath("$.detail").isEqualTo("An employee with this name already exists in this office.");
    }

    @Test
    void createEmployee_Returns_BadRequest_When_Request_Is_Invalid() {
        restTestClient.post()
                .body(new CreateEmployeeDTO("", null))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Validation Error")
                .jsonPath("$.detail").isEqualTo("Validation failed for one or more fields")
                .jsonPath("$.errors.name").isEqualTo("Naam moet tussen 1 en 255 karakters zijn.")
                .jsonPath("$.errors.office").isEqualTo("Kantoor is verplicht.");
    }

    @Test
    void updateEmployee_Returns_NotFound_When_Employee_Does_Not_Exist() {
        restTestClient.put().uri("/{id}", 99L)
                .body(new UpdateEmployeeDTO("Updated", Office.AMERSFOORT))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Entity Not Found")
                .jsonPath("$.detail").isEqualTo("Entity not found");
    }

    @Test
    void updateEmployee_Returns_Conflict_When_New_Name_Already_Exists_In_Office() {
        createEmployee("Alex", Office.DENBOSCH);
        var target = createEmployee("Luca", Office.DENBOSCH);

        restTestClient.put().uri("/{id}", target.id())
                .body(new UpdateEmployeeDTO("Alex", Office.DENBOSCH))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Employee already exists")
                .jsonPath("$.detail").isEqualTo("An employee with this name already exists in this office.");
    }

    @Test
    void updateEmployee_Returns_BadRequest_When_Request_Is_Invalid() {
        var created = createEmployee("Mika", Office.GRONINGEN);

        restTestClient.put().uri("/{id}", created.id())
                .body(new UpdateEmployeeDTO("", null))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Validation Error")
                .jsonPath("$.detail").isEqualTo("Validation failed for one or more fields")
                .jsonPath("$.errors.name").isEqualTo("Naam moet tussen 1 en 255 karakters zijn.")
                .jsonPath("$.errors.office").isEqualTo("Kantoor is verplicht.");
    }

    @Test
    void deleteEmployee_Returns_NotFound_When_Employee_Does_Not_Exist() {
        restTestClient.delete().uri("/{id}", 999L).exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Entity Not Found")
                .jsonPath("$.detail").isEqualTo("Entity not found");
    }

    private EmployeeDTO createEmployee(String name, Office office) {
        var result = restTestClient.post()
                .body(new CreateEmployeeDTO(name, office))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody(EmployeeDTO.class)
                .returnResult();

        var employee = result.getResponseBody();
        assertThat(employee).isNotNull();
        assertThat(employee.id()).isNotNull();
        assertThat(employee.name()).isEqualTo(name);
        assertThat(employee.office()).isEqualTo(office);
        assertThat(result.getResponseHeaders().getLocation()).hasPath("/api/employees/" + employee.id());

        return employee;
    }
}
