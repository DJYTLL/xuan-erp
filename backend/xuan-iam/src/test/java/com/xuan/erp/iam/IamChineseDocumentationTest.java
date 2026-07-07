package com.xuan.erp.iam;

import com.xuan.erp.iam.interfaces.dto.CreateIamUserRequest;
import com.xuan.erp.iam.interfaces.dto.DisableIamUserRequest;
import com.xuan.erp.iam.interfaces.dto.IamAuthorizationSnapshotResponse;
import com.xuan.erp.iam.interfaces.dto.IamUserResponse;
import com.xuan.erp.iam.interfaces.dto.RebuildAuthorizationSnapshotRequest;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IamChineseDocumentationTest {

    private static final Pattern PUBLIC_TYPE = Pattern.compile("public\\s+(?:final\\s+)?(?:record|class|enum|interface)\\s+([A-Za-z0-9_]+)");
    private static final Pattern CHINESE = Pattern.compile("[\\u4e00-\\u9fa5]");

    @Test
    void allIamPublicTypesHaveChineseJavadocs() throws IOException {
        Path sourceRoot = Path.of("src/main/java/com/xuan/erp/iam");
        List<String> missing = new ArrayList<>();

        try (var files = Files.walk(sourceRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                Matcher matcher = PUBLIC_TYPE.matcher(source);
                while (matcher.find()) {
                    if (!hasChineseJavadocBefore(source, matcher.start())) {
                        missing.add(sourceRoot.relativize(file) + "#" + matcher.group(1));
                    }
                }
            }
        }

        assertTrue(missing.isEmpty(), "以下 IAM 公开类型缺少中文 Javadoc：" + missing);
    }

    @Test
    void iamDtosExposeChineseSchemaDescriptions() {
        List<Class<?>> dtos = List.of(
                CreateIamUserRequest.class,
                DisableIamUserRequest.class,
                RebuildAuthorizationSnapshotRequest.class,
                IamUserResponse.class,
                IamAuthorizationSnapshotResponse.class
        );
        List<String> missing = new ArrayList<>();

        for (Class<?> dto : dtos) {
            Schema typeSchema = dto.getAnnotation(Schema.class);
            if (typeSchema == null || !hasChinese(typeSchema.description())) {
                missing.add(dto.getSimpleName() + " 缺少类型级中文 @Schema");
            }
            for (RecordComponent component : dto.getRecordComponents()) {
                Schema fieldSchema = schemaOnRecordComponent(dto, component);
                if (fieldSchema == null || !hasChinese(fieldSchema.description())) {
                    missing.add(dto.getSimpleName() + "." + component.getName() + " 缺少字段级中文 @Schema");
                }
            }
        }

        assertTrue(missing.isEmpty(), String.join("；", missing));
    }

    @Test
    void iamControllerPathAndQueryParametersExposeChineseDescriptions() {
        List<String> missing = new ArrayList<>();

        for (Class<?> controller : controllerClasses()) {
            assertTrue(controller.isAnnotationPresent(RestController.class), controller.getSimpleName() + " 缺少 @RestController");
            for (Method method : controller.getDeclaredMethods()) {
                java.lang.reflect.Parameter[] parameters = method.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    java.lang.reflect.Parameter parameter = parameters[i];
                    if (!hasAnnotation(parameter, PathVariable.class) && !hasAnnotation(parameter, RequestParam.class)) {
                        continue;
                    }
                    Parameter apiParameter = parameter.getAnnotation(Parameter.class);
                    if (apiParameter == null || !hasChinese(apiParameter.description())) {
                        missing.add(controller.getSimpleName() + "#" + method.getName() + " 参数 " + i);
                    }
                }
            }
        }

        assertTrue(missing.isEmpty(), "以下 IAM Controller 参数缺少中文 @Parameter：" + missing);
    }

    private boolean hasChineseJavadocBefore(String source, int typeStart) {
        String beforeType = source.substring(0, typeStart);
        int docStart = beforeType.lastIndexOf("/**");
        int docEnd = beforeType.lastIndexOf("*/");
        return docStart >= 0 && docEnd > docStart && hasChinese(beforeType.substring(docStart, docEnd));
    }

    private static boolean hasChinese(String value) {
        return value != null && CHINESE.matcher(value).find();
    }

    private boolean hasAnnotation(java.lang.reflect.Parameter parameter, Class<? extends Annotation> annotationType) {
        return parameter.getAnnotation(annotationType) != null;
    }

    private Schema schemaOnRecordComponent(Class<?> dto, RecordComponent component) {
        Schema schema = component.getAnnotation(Schema.class);
        if (schema != null) {
            return schema;
        }
        schema = component.getAccessor().getAnnotation(Schema.class);
        if (schema != null) {
            return schema;
        }
        try {
            return dto.getDeclaredField(component.getName()).getAnnotation(Schema.class);
        } catch (NoSuchFieldException ex) {
            return null;
        }
    }

    private List<Class<?>> controllerClasses() {
        return List.of(
                com.xuan.erp.iam.interfaces.controller.IamAuthorizationController.class,
                com.xuan.erp.iam.interfaces.controller.IamMenuController.class,
                com.xuan.erp.iam.interfaces.controller.IamPermissionController.class,
                com.xuan.erp.iam.interfaces.controller.IamRoleController.class,
                com.xuan.erp.iam.interfaces.controller.IamTenantBootstrapController.class,
                com.xuan.erp.iam.interfaces.controller.IamUserController.class
        );
    }
}
