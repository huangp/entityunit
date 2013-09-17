package com.github.huangp.makeit.entity;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

/**
 * @author Patrick Huang
 */
@RequiredArgsConstructor(staticName = "of")
@EqualsAndHashCode
class CacheKey
{
   private final Class type;
   private final ScanOption option;
}
