package nl.quintor.soqqer.employee.persistence.repository;

import nl.quintor.soqqer.employee.persistence.entity.Employee;
import nl.quintor.soqqer.employee.persistence.entity.Office;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    boolean existsByNameAndOffice(String name, Office office);

    boolean existsByNameAndOfficeAndIdNot(String name, Office office, Long id);
}
