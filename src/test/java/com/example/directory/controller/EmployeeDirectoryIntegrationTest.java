package com.example.directory.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "directory.max-skills-per-employee=2",
        "pagination.default-page-size=5"
})
@AutoConfigureMockMvc
@Transactional
class EmployeeDirectoryIntegrationTest {

    @Autowired MockMvc mvc;

    // ---------- helpers ----------

    private long createAndGetId(String url, String json) throws Exception {
        String body = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long department(String name) throws Exception {
        return createAndGetId("/api/departments", "{\"name\":\"" + name + "\",\"location\":\"Bengaluru\"}");
    }

    private long skill(String name, String category) throws Exception {
        return createAndGetId("/api/skills", "{\"name\":\"" + name + "\",\"category\":\"" + category + "\"}");
    }

    private String employeeJson(String name, String email, long departmentId, String skillIdsJson) {
        return "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"designation\":\"Engineer\","
                + "\"joiningDate\":\"2023-04-01\",\"departmentId\":" + departmentId
                + ",\"skillIds\":" + skillIdsJson + "}";
    }

    // ---------- tests ----------

    @Test
    void info_exposesValuesFromProperties() throws Exception {
        mvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Employee Directory Service"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.maxSkillsPerEmployee").value(2))
                .andExpect(jsonPath("$.defaultPageSize").value(5));
    }

    @Test
    void createEmployeeWithSkills_thenReadFilterAndHeadCount() throws Exception {
        long dept = department("Engineering");
        long java = skill("Java", "Backend");
        long sql = skill("SQL", "Data");
        long empId = createAndGetId("/api/employees",
                employeeJson("Asha Rao", "asha@corp.com", dept, "[" + java + "," + sql + "]"));

        mvc.perform(get("/api/employees/{id}", empId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentName").value("Engineering"))
                .andExpect(jsonPath("$.skills.length()").value(2));

        mvc.perform(get("/api/employees").param("skillId", String.valueOf(java)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("asha@corp.com"));

        mvc.perform(get("/api/employees").param("skill", "sql").param("departmentId", String.valueOf(dept)))
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(get("/api/employees").param("designation", "Manager"))
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(get("/api/departments/{id}", dept))
                .andExpect(jsonPath("$.headCount").value(1));

        mvc.perform(get("/api/departments/{id}/employees", dept))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void invalidEmployee_returns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/employees"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(4)); // name, email, joiningDate, departmentId
    }

    @Test
    void malformedDate_returns400() throws Exception {
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"email\":\"a@b.com\",\"joiningDate\":\"yesterday\",\"departmentId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void duplicateEmail_returns409() throws Exception {
        long dept = department("Engineering");
        createAndGetId("/api/employees", employeeJson("Asha", "asha@corp.com", dept, "[]"));

        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("Other", "ASHA@corp.com", dept, "[]")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void unknownEmployee_returns404WithMessage() throws Exception {
        mvc.perform(get("/api/employees/{id}", 987654))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee with id 987654 not found"));
    }

    @Test
    void employeeInUnknownDepartment_returns404() throws Exception {
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("Asha", "asha@corp.com", 987654, "[]")))
                .andExpect(status().isNotFound());
    }

    @Test
    void addSkillBeyondConfiguredLimit_returns422() throws Exception {
        long dept = department("Engineering");
        long java = skill("Java", "Backend");
        long sql = skill("SQL", "Data");
        long react = skill("React", "Frontend");
        long empId = createAndGetId("/api/employees",
                employeeJson("Asha", "asha@corp.com", dept, "[" + java + "," + sql + "]"));

        mvc.perform(post("/api/employees/{id}/skills/{sid}", empId, react))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Employee " + empId + " already has the maximum of 2 skills"));
    }

    @Test
    void addAndRemoveSkill_roundTrip() throws Exception {
        long dept = department("Engineering");
        long java = skill("Java", "Backend");
        long empId = createAndGetId("/api/employees", employeeJson("Asha", "asha@corp.com", dept, "[]"));

        mvc.perform(post("/api/employees/{id}/skills/{sid}", empId, java))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills.length()").value(1));

        mvc.perform(post("/api/employees/{id}/skills/{sid}", empId, java))
                .andExpect(status().isConflict());

        mvc.perform(delete("/api/employees/{id}/skills/{sid}", empId, java))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills.length()").value(0));

        mvc.perform(delete("/api/employees/{id}/skills/{sid}", empId, java))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDepartmentWithEmployees_returns409_thenSucceedsOnceEmpty() throws Exception {
        long dept = department("Engineering");
        long empId = createAndGetId("/api/employees", employeeJson("Asha", "asha@corp.com", dept, "[]"));

        mvc.perform(delete("/api/departments/{id}", dept)).andExpect(status().isConflict());
        mvc.perform(delete("/api/employees/{id}", empId)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/departments/{id}", dept)).andExpect(status().isNoContent());
        mvc.perform(get("/api/departments/{id}", dept)).andExpect(status().isNotFound());
    }

    @Test
    void invalidSortField_returns400() throws Exception {
        mvc.perform(get("/api/employees").param("sort", "password,asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pagination_usesConfiguredDefaultAndHonoursSize() throws Exception {
        long dept = department("Engineering");
        for (int i = 0; i < 7; i++) {
            createAndGetId("/api/employees", employeeJson("Emp" + i, "emp" + i + "@corp.com", dept, "[]"));
        }

        mvc.perform(get("/api/employees"))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(7))
                .andExpect(jsonPath("$.totalPages").value(2));

        mvc.perform(get("/api/employees").param("size", "3").param("page", "2"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }
}
