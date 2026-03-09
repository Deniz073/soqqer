package nl.quintor.soqqer.employee.persistence.service;

import jakarta.persistence.EntityNotFoundException;
import nl.quintor.soqqer.employee.EmployeeMTO;
import nl.quintor.soqqer.employee.gateway.api.dto.CreateEmployeeDTO;
import nl.quintor.soqqer.employee.gateway.api.dto.EmployeeDTO;
import nl.quintor.soqqer.employee.gateway.api.dto.EmployeeSelectDTO;
import nl.quintor.soqqer.employee.gateway.api.dto.UpdateEmployeeDTO;
import nl.quintor.soqqer.employee.persistence.entity.Employee;
import nl.quintor.soqqer.employee.persistence.entity.Office;
import nl.quintor.soqqer.employee.persistence.exception.EmployeeAlreadyExistsException;
import nl.quintor.soqqer.employee.persistence.mapper.EmployeeMapper;
import nl.quintor.soqqer.employee.persistence.repository.EmployeeRepository;
import nl.quintor.soqqer.common.events.match.MatchFinishedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeeMapper employeeMapper;
    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void findAll_Returns_Employees_As_EmployeeDTO() {
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "elo"));
        var employee1 = Employee.builder().name("test").build();
        var employee2 = Employee.builder().name("test2").build();

        when(employeeRepository.findAll(pageable)).thenReturn(
                new PageImpl<>(List.of(
                        employee1,
                        employee2
                ))

        );

        when(employeeMapper.toDTO(employee1)).thenReturn(new EmployeeDTO(1L, "test", null, 1000, 0));
        when(employeeMapper.toDTO(employee2)).thenReturn(new EmployeeDTO(2L, "test2", null, 1000, 0));

        var result = employeeService.find(pageable);

        assertThat(result).hasSize(2)
                .extracting(EmployeeDTO::name)
                .containsExactly("test", "test2");

    }

    @Test
    void create_Throws_When_Employee_Already_Exists() {
        var dto = new CreateEmployeeDTO("test", Office.DENBOSCH);

        when(employeeRepository.existsByNameAndOffice(dto.name(), dto.office())).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(dto))
                .isInstanceOf(EmployeeAlreadyExistsException.class)
                .hasMessage("An employee with this name already exists in this office.");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void findForSelect_Returns_Minimal_EmployeeDTO_List() {
        var first = Employee.builder().name("Alex").office(Office.DENBOSCH).build();
        first.setId(1L);
        var second = Employee.builder().name("Zoe").office(Office.DENHAAG).build();
        second.setId(2L);

        var selectDTOs = List.of(
                new EmployeeSelectDTO(1L, "Alex", "Den Bosch"),
                new EmployeeSelectDTO(2L, "Zoe", "Den Haag")
        );

        when(employeeRepository.findAll(eq(Sort.by("name")))).thenReturn(List.of(first, second));
        when(employeeMapper.toSelectDTO(List.of(first, second))).thenReturn(selectDTOs);

        var result = employeeService.findForSelect();

        assertThat(result).isEqualTo(selectDTOs);
        verify(employeeRepository).findAll(eq(Sort.by("name")));
        verify(employeeMapper).toSelectDTO(List.of(first, second));
    }

    @Test
    void create_Saves_And_Returns_EmployeeDTO() {
        var dto = new CreateEmployeeDTO("test", Office.DENBOSCH);
        var entity = Employee.builder().name("test").office(Office.DENBOSCH).build();
        entity.setId(1L);
        var mappedDto = new EmployeeDTO(1L, "test", "Den Bosch", 1000, 0);

        when(employeeRepository.existsByNameAndOffice(dto.name(), dto.office())).thenReturn(false);
        when(employeeMapper.toEntity(dto)).thenReturn(entity);
        when(employeeRepository.save(entity)).thenReturn(entity);
        when(employeeMapper.toDTO(entity)).thenReturn(mappedDto);

        var result = employeeService.create(dto);

        assertThat(result).isEqualTo(mappedDto);
        verify(employeeRepository).save(entity);
    }

    @Test
    void findById_Returns_EmployeeDTO_When_Found() {
        var employee = Employee.builder().name("test").office(Office.DENBOSCH).build();
        employee.setId(1L);
        var dto = new EmployeeDTO(1L, "test", "Den Bosch", 1000, 0);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeMapper.toDTO(employee)).thenReturn(dto);

        var result = employeeService.findById(1L);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void findById_Throws_When_Not_Found() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Employee with id 99 was not found.");
    }

    @Test
    void update_Throws_When_Employee_Not_Found() {
        var dto = new UpdateEmployeeDTO("updated", Office.DENHAAG);
        when(employeeRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.update(10L, dto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Employee with id 10 was not found.");
    }

    @Test
    void update_Throws_When_Duplicate_Name_Exists() {
        var dto = new UpdateEmployeeDTO("updated", Office.DENHAAG);
        var existing = Employee.builder().name("existing").office(Office.DENHAAG).build();
        existing.setId(5L);

        when(employeeRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(employeeRepository.existsByNameAndOfficeAndIdNot(dto.name(), dto.office(), 5L)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.update(5L, dto))
                .isInstanceOf(EmployeeAlreadyExistsException.class)
                .hasMessage("An employee with this name already exists in this office.");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void update_Saves_And_Returns_EmployeeDTO() {
        var dto = new UpdateEmployeeDTO("updated", Office.DENHAAG);
        var employee = Employee.builder().name("original").office(Office.DENBOSCH).build();
        employee.setId(5L);
        var updatedEmployee = Employee.builder().name("updated").office(Office.DENHAAG).build();
        updatedEmployee.setId(5L);
        var mappedDto = new EmployeeDTO(5L, "updated", "Den Haag", 1000, 0);

        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByNameAndOfficeAndIdNot(dto.name(), dto.office(), 5L)).thenReturn(false);
        when(employeeMapper.update(dto, employee)).thenReturn(updatedEmployee);
        when(employeeRepository.save(updatedEmployee)).thenReturn(updatedEmployee);
        when(employeeMapper.toDTO(updatedEmployee)).thenReturn(mappedDto);

        var result = employeeService.update(5L, dto);

        assertThat(result).isEqualTo(mappedDto);
        verify(employeeRepository).save(updatedEmployee);
    }

    @Test
    void delete_Deletes_When_Employee_Exists() {
        var employee = Employee.builder().name("to-delete").office(Office.DEVENTER).build();
        employee.setId(3L);

        when(employeeRepository.findById(3L)).thenReturn(Optional.of(employee));

        employeeService.delete(3L);

        verify(employeeRepository).delete(employee);
    }

    @Test
    void delete_Throws_When_Employee_Not_Found() {
        when(employeeRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.delete(3L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Employee with id 3 was not found.");
    }

    @Test
    void findMissingEmployeeIds_Returns_Empty_Set_For_Null_Input() {
        var result = employeeService.findMissingEmployeeIds(null);

        assertThat(result).isEmpty();
        verify(employeeRepository, never()).findAllById(any());
    }

    @Test
    void findMissingEmployeeIds_Returns_Empty_Set_For_Empty_Input() {
        var result = employeeService.findMissingEmployeeIds(Set.of());

        assertThat(result).isEmpty();
        verify(employeeRepository, never()).findAllById(any());
    }

    @Test
    void findMissingEmployeeIds_Returns_Ids_Not_Found_In_Repository() {
        var existingOne = Employee.builder().name("one").office(Office.DENBOSCH).build();
        existingOne.setId(1L);
        var existingThree = Employee.builder().name("three").office(Office.DENHAAG).build();
        existingThree.setId(3L);

        when(employeeRepository.findAllById(Set.of(1L, 2L, 3L))).thenReturn(List.of(existingOne, existingThree));

        var result = employeeService.findMissingEmployeeIds(Set.of(1L, 2L, 3L));

        assertThat(result).containsExactly(2L);
    }

    @Test
    void findEmployees_Returns_Map_With_Mapped_EmployeeMTOs() {
        var first = Employee.builder().name("test").office(Office.DENBOSCH).build();
        first.setId(1L);
        var second = Employee.builder().name("test2").office(Office.DENHAAG).build();
        second.setId(2L);

        when(employeeRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(first, second));
        when(employeeMapper.toMTO(first)).thenReturn(new EmployeeMTO("test", "Den Bosch", 1000, 0));
        when(employeeMapper.toMTO(second)).thenReturn(new EmployeeMTO("test2", "Den Haag", 1000, 0));

        var result = employeeService.findEmployees(Set.of(1L, 2L));

        assertThat(result)
                .isEqualTo(Map.of(
                        1L, new EmployeeMTO("test", "Den Bosch", 1000, 0),
                        2L, new EmployeeMTO("test2", "Den Haag", 1000, 0)
                ));
    }

    @Test
    void calculateNewPlayerElos_UpdatesRatings_For1v1Match() {
        var p1 = Employee.builder().name("A").office(Office.DENBOSCH).elo(1000).build();
        p1.setId(1L);
        var p2 = Employee.builder().name("B").office(Office.DENHAAG).elo(1000).build();
        p2.setId(2L);

        var event = new MatchFinishedEvent(10, 8, List.of(1L), List.of(2L));

        when(employeeRepository.findAllById(Set.of(1L, 2L)))
                .thenReturn(List.of(p1, p2));

        employeeService.calculateNewPlayerElos(event);

        assertThat(p1.getElo()).isEqualTo(1010);
        assertThat(p2.getElo()).isEqualTo(990);

        verify(employeeRepository).saveAll(any());
    }

    @Test
    void calculateNewPlayerElos_UpdatesRatings_For2v2Match() {
        var a1 = Employee.builder().name("A1").office(Office.DENBOSCH).elo(1000).build();
        a1.setId(1L);
        var a2 = Employee.builder().name("A2").office(Office.DENBOSCH).elo(1000).build();
        a2.setId(2L);

        var b1 = Employee.builder().name("B1").office(Office.DENHAAG).elo(1000).build();
        b1.setId(3L);
        var b2 = Employee.builder().name("B2").office(Office.DENHAAG).elo(1000).build();
        b2.setId(4L);

        var event = new MatchFinishedEvent(10, 6, List.of(1L, 2L), List.of(3L, 4L));

        when(employeeRepository.findAllById(Set.of(1L, 2L, 3L, 4L)))
                .thenReturn(List.of(a1, a2, b1, b2));

        employeeService.calculateNewPlayerElos(event);

        assertThat(a1.getElo()).isEqualTo(1010);
        assertThat(a2.getElo()).isEqualTo(1010);
        assertThat(b1.getElo()).isEqualTo(990);
        assertThat(b2.getElo()).isEqualTo(990);

        verify(employeeRepository).saveAll(any());
    }

    @Test
    void calculateNewPlayerElos_UnderdogWin_GivesBiggerRatingGain() {
        var a = Employee.builder().name("A").office(Office.DENBOSCH).elo(1000).build();
        a.setId(1L);

        var b = Employee.builder().name("B").office(Office.DENHAAG).elo(1200).build();
        b.setId(2L);

        var event = new MatchFinishedEvent(10, 8, List.of(1L), List.of(2L));

        when(employeeRepository.findAllById(Set.of(1L, 2L)))
                .thenReturn(List.of(a, b));

        employeeService.calculateNewPlayerElos(event);

        assertThat(a.getElo()).isGreaterThan(1010);
        assertThat(b.getElo()).isLessThan(1190);

        verify(employeeRepository).saveAll(any());
    }

    @Test
    void calculateNewPlayerElos_Draw_AdjustsRatingsTowardEachOther() {
        var a = Employee.builder().name("A").office(Office.DENBOSCH).elo(1200).build();
        a.setId(1L);

        var b = Employee.builder().name("B").office(Office.DENHAAG).elo(1000).build();
        b.setId(2L);

        var event = new MatchFinishedEvent(9, 9, List.of(1L), List.of(2L));

        when(employeeRepository.findAllById(Set.of(1L, 2L)))
                .thenReturn(List.of(a, b));

        employeeService.calculateNewPlayerElos(event);

        assertThat(a.getElo()).isLessThan(1200);
        assertThat(b.getElo()).isGreaterThan(1000);

        verify(employeeRepository).saveAll(any());
    }
}
