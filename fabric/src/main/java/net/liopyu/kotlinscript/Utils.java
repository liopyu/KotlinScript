package net.liopyu.kotlinscript;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class Utils {

    public void withBasicAnnotation(@NotNull String input) {
    }

    public void withValueAnnotation(@Named("id") String id) {
    }

    public void withMultipleAnnotations(
            @Nullable(value = "something") @Size(max = 10) List<String> values) {
    }

    public void withQualifiedAnnotation(@org.jetbrains.annotations.NotNull String name) {
    }

    public void withArrayAnnotation(@Tag({"tag1", "tag2"}) String label) {
    }

    public void withNestedAnnotation(@JsonProperty(required = true) int count) {
    }

    public void withAnnotationAndGenerics(@Nullable List<@NotNull String> names) {
    }

    public void withAnnotationAndWildcard(@MyAnno(description = "? extends Foo") Class<?> foo) {
    }

    public void withComplexParam(
            @AnnotationWithDefault(value = "default", flag = true) Map<String, Object> map) {
    }

    public void withNoArgsAnnotation(@Deprecated int unused) {
    }

    public void withCustomAnnotation(
            @KDoc("Some custom doc string") PoseStack poseStack) {
    }

    public void withAnnotationOnArray(@NotEmpty String[] array) {
    }

    public void withKotlinInterop(@org.jetbrains.annotations.Nullable("") Object maybe) {
    }

    public void withMixedAnnotations(
            @Nullable
            @Named("target")
            @SuppressWarnings("unchecked")
            Object mixed) {
    }
}

