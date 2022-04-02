package io.github.ikws4.buildergeneratordemo;

import io.github.ikws4.buildergenerator.Builder;
import io.github.ikws4.buildergenerator.BuilderProperty;

/**
 * Created by zhiping on 04/01/2022.
 */
@Builder
public class Info {
  @BuilderProperty
  String name;

  @BuilderProperty
  String title;

  @BuilderProperty
  int count;
}
