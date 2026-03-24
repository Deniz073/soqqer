package nl.quintor.soqqer.employee;

import java.util.Map;
import java.util.Set;

public interface EmployeeLookup {
    Set<Long> findMissingEmployeeIds(Set<Long> employeeIds);
    Map<Long, EmployeeMTO> findEmployees(Set<Long> employeeIds);
}
