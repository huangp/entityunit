package com.github.huangp.makeit.entity;

import com.github.huangp.makeit.holder.BeanValueHolder;
import com.github.huangp.makeit.maker.PreferredValueMakersRegistry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Patrick Huang
 */
@RequiredArgsConstructor
@Getter
public class MakeContext
{
   private final BeanValueHolder beanValueHolder;
   private final PreferredValueMakersRegistry preferredValueMakers;

}
