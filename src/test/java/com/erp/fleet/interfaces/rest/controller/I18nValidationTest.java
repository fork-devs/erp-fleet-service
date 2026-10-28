package com.erp.fleet.interfaces.rest.controller;

import com.erp.fleet.application.usecase.DeactivateCustomerUseCase;
import com.erp.fleet.application.usecase.GetCustomerUseCase;
import com.erp.fleet.application.usecase.RegisterCustomerUseCase;
import com.erp.fleet.application.usecase.RetryValidationUseCase;
import com.erp.fleet.application.usecase.UpdateCustomerUseCase;
import com.erp.fleet.infrastructure.config.I18nConfig;
import com.erp.fleet.interfaces.rest.advice.GlobalRestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class I18nValidationTest {

    private MockMvc mockMvc;

    @Mock
    private RegisterCustomerUseCase registerCustomerUseCase;
    @Mock
    private GetCustomerUseCase getCustomerUseCase;
    @Mock
    private UpdateCustomerUseCase updateCustomerUseCase;
    @Mock
    private DeactivateCustomerUseCase deactivateCustomerUseCase;
    @Mock
    private RetryValidationUseCase retryValidationUseCase;

    private static final String INVALID_PAYLOAD = """
            {
                "fullName": "",
                "docType": "DNI",
                "docNumber": "12345678"
            }
            """;

    @BeforeEach
    void setUp() {
        I18nConfig config = new I18nConfig();
        MessageSource messageSource = config.messageSource();
        LocalValidatorFactoryBean validator = config.getValidator(messageSource);
        validator.afterPropertiesSet();

        CustomerController controller = new CustomerController(
                registerCustomerUseCase,
                getCustomerUseCase,
                updateCustomerUseCase,
                deactivateCustomerUseCase,
                retryValidationUseCase
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .setLocaleResolver(config.localeResolver())
                .setControllerAdvice(new GlobalRestExceptionHandler(messageSource))
                .build();
    }

    @Test
    @DisplayName("Accept-Language: es debe retornar mensaje de validación en español")
    void shouldReturnValidationErrorsInSpanish() throws Exception {
        mockMvc.perform(post("/api/v1/fleet/customers")
                        .header("Accept-Language", "es")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_PAYLOAD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de Validación"))
                .andExpect(jsonPath("$.detail").value("fullName: El nombre completo es obligatorio."));
    }

    @Test
    @DisplayName("Accept-Language: en debe retornar mensaje de validación en inglés")
    void shouldReturnValidationErrorsInEnglish() throws Exception {
        mockMvc.perform(post("/api/v1/fleet/customers")
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_PAYLOAD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.detail").value("fullName: Full name is required."));
    }

    @Test
    @DisplayName("Sin header Accept-Language debe usar locale por defecto (es)")
    void shouldFallbackToDefaultLocaleWhenHeaderMissing() throws Exception {
        mockMvc.perform(post("/api/v1/fleet/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_PAYLOAD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de Validación"))
                .andExpect(jsonPath("$.detail").value("fullName: El nombre completo es obligatorio."));
    }
}
