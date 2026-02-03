package com.eventsApp.employee;

import com.eventsApp.employee.model.dto.EmployeeDTO;
import com.eventsApp.employee.model.command.EmployeeCreateCommand;
import com.eventsApp.employee.model.command.EmployeeUpdateCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping()
    public ResponseEntity<Page<EmployeeDTO>> getAll(@PageableDefault Pageable pageable) {
        return new ResponseEntity(employeeService.getAll(pageable), HttpStatus.OK);
    }

    @PostMapping()
    public ResponseEntity<EmployeeDTO> create(@RequestBody EmployeeCreateCommand command) {
        return new ResponseEntity(employeeService.create(command), HttpStatus.CREATED);

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        employeeService.delete(id);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable int id, @RequestBody EmployeeUpdateCommand command) {
        return new ResponseEntity(employeeService.update(id, command), HttpStatus.OK);

    }
}

