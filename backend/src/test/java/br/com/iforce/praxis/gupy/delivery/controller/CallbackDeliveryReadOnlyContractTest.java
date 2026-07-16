package br.com.iforce.praxis.gupy.delivery.controller;

import br.com.iforce.praxis.gupy.delivery.service.CallbackDeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class CallbackDeliveryReadOnlyContractTest {

    @Test
    void callbackDeliveryHistoryDoesNotExposeServerSideReprocessing() {
        assertThat(methodsAnnotatedWith(CallbackDeliveryController.class, PostMapping.class)).isEmpty();
        assertThat(methodsAnnotatedWith(CallbackDeliveryController.class, GetMapping.class)).hasSize(1);
        assertThat(Arrays.stream(CallbackDeliveryService.class.getDeclaredMethods())
                .map(Method::getName))
                .doesNotContain("reprocess");
    }

    private Method[] methodsAnnotatedWith(
            Class<?> type,
            Class<? extends java.lang.annotation.Annotation> annotation
    ) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotation))
                .toArray(Method[]::new);
    }
}
