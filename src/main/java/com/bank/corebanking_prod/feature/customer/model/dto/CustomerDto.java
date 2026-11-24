
package com.bank.corebanking_prod.feature.customer.model.dto;

import com.bank.corebanking_prod.common.validation.tckn.ValidTckn;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CustomerDto {

    private CustomerDto() {}

    public static class Create {

        @ValidTckn
        private String tckn;

        @NotBlank(message = "First name cannot be blank.")
        @Size(max = 50, message = "First name cannot exceed 50 characters.")
        private String firstName;

        @NotBlank(message = "Last name cannot be blank.")
        @Size(max = 50, message = "Last name cannot exceed 50 characters.")
        private String lastName;

        @NotBlank(message = "Email cannot be blank.")
        @Email(message = "Please provide a valid email address.")
        @Size(max = 254, message = "Email cannot exceed 254 characters.")
        private String email;

        @NotBlank(message = "Phone number cannot be blank.")
        @Pattern(
                regexp = "^(\\+90|0)?5[0-9]{9}$",
                message = "Please provide a valid Turkish mobile phone number."
        )
        private String phoneNumber;

        public Create() {}

        public Create(
                String tckn,
                String firstName,
                String lastName,
                String email,
                String phoneNumber) {

            this.tckn = tckn;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.phoneNumber = phoneNumber;
        }

        public String getTckn() {
            return tckn;
        }

        public void setTckn(String tckn) {
            this.tckn = tckn;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }

    public static class Update {

        @NotBlank(message = "First name cannot be blank.")
        @Size(max = 50, message = "First name cannot exceed 50 characters.")
        private String firstName;

        @NotBlank(message = "Last name cannot be blank.")
        @Size(max = 50, message = "Last name cannot exceed 50 characters.")
        private String lastName;

        @NotBlank(message = "Email cannot be blank.")
        @Email(message = "Please provide a valid email address.")
        @Size(max = 254, message = "Email cannot exceed 254 characters.")
        private String email;

        @NotBlank(message = "Phone number cannot be blank.")
        @Pattern(
                regexp = "^(\\+90|0)?5[0-9]{9}$",
                message = "Please provide a valid Turkish mobile phone number."
        )
        private String phoneNumber;

        public Update() {}

        public Update(
                String firstName,
                String lastName,
                String email,
                String phoneNumber) {

            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.phoneNumber = phoneNumber;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }

    public static class Response {

        private Long id;
        private String tckn;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;

        public Response() {}

        public Response(
                Long id,
                String tckn,
                String firstName,
                String lastName,
                String email,
                String phoneNumber) {

            this.id = id;
            this.tckn = tckn;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.phoneNumber = phoneNumber;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTckn() {
            return tckn;
        }

        public void setTckn(String tckn) {
            this.tckn = tckn;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }
}
