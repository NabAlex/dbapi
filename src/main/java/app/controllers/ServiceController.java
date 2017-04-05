package app.controllers;

import app.service.ServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/service")
public class ServiceController {

    final private ServiceService serviceService;

    @Autowired
    public ServiceController(ServiceService serviceService){
        this.serviceService = serviceService;
    }

    @RequestMapping(path = "/clear", method = RequestMethod.POST)
    public ResponseEntity clear(){
        serviceService.dropAllTables();
        serviceService.createAllTables();
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @RequestMapping(path = "/status", method = RequestMethod.GET)
    public ResponseEntity getStatus(){
        return ResponseEntity.ok(serviceService.getStatus());
    }
}
