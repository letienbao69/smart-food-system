package com.food.smart_food_system.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeeDTO {
    private Long id; private String employeeCode; private String fullName; private String gender; private LocalDate dateOfBirth;
    private String phone; private String email; private String address; private LocalDate hireDate; private Long positionId; private String positionName;
    private BigDecimal salary; private String shiftName; private String status; private String note;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getEmployeeCode(){return employeeCode;} public void setEmployeeCode(String employeeCode){this.employeeCode=employeeCode;}
    public String getFullName(){return fullName;} public void setFullName(String fullName){this.fullName=fullName;}
    public String getGender(){return gender;} public void setGender(String gender){this.gender=gender;}
    public LocalDate getDateOfBirth(){return dateOfBirth;} public void setDateOfBirth(LocalDate dateOfBirth){this.dateOfBirth=dateOfBirth;}
    public String getPhone(){return phone;} public void setPhone(String phone){this.phone=phone;}
    public String getEmail(){return email;} public void setEmail(String email){this.email=email;}
    public String getAddress(){return address;} public void setAddress(String address){this.address=address;}
    public LocalDate getHireDate(){return hireDate;} public void setHireDate(LocalDate hireDate){this.hireDate=hireDate;}
    public Long getPositionId(){return positionId;} public void setPositionId(Long positionId){this.positionId=positionId;}
    public String getPositionName(){return positionName;} public void setPositionName(String positionName){this.positionName=positionName;}
    public BigDecimal getSalary(){return salary;} public void setSalary(BigDecimal salary){this.salary=salary;}
    public String getShiftName(){return shiftName;} public void setShiftName(String shiftName){this.shiftName=shiftName;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public String getNote(){return note;} public void setNote(String note){this.note=note;}
}
