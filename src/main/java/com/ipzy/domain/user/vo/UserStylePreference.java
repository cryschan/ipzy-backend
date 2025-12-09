package com.ipzy.domain.user.vo;

import com.ipzy._global.common.enums.Gender;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStylePreference {

    private List<String> colors = new ArrayList<>();

    private Integer age;

    private Gender gender;

    private List<String> styles = new ArrayList<>();

    @Builder
    public UserStylePreference(List<String> colors, Integer age, Gender gender, List<String> styles) {
        this.colors = colors != null ? colors : new ArrayList<>();
        this.age = age;
        this.gender = gender;
        this.styles = styles != null ? styles : new ArrayList<>();
    }
}