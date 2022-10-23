/*
 * Copyright (c) 2022 - present Cristian Donoiu, Ionut Sergiu Peschir
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongopipe.core.runner.command.param;

import com.mongodb.client.model.CollationAlternate;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;

public class Collation {
  private String locale;
  private Boolean caseLevel;
  private CollationCaseFirst caseFirst;
  private CollationStrength strength;
  private Boolean numericOrdering;
  private CollationAlternate alternate;
  private CollationMaxVariable maxVariable;
  private Boolean normalization;
  private Boolean backwards;

  public Collation() {
  }

  private Collation(Builder builder) {
    setLocale(builder.locale);
    setCaseLevel(builder.caseLevel);
    setCaseFirst(builder.caseFirst);
    setStrength(builder.strength);
    setNumericOrdering(builder.numericOrdering);
    setAlternate(builder.alternate);
    setMaxVariable(builder.maxVariable);
    setNormalization(builder.normalization);
    setBackwards(builder.backwards);
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public Boolean getCaseLevel() {
    return caseLevel;
  }

  public void setCaseLevel(Boolean caseLevel) {
    this.caseLevel = caseLevel;
  }

  public CollationCaseFirst getCaseFirst() {
    return caseFirst;
  }

  public void setCaseFirst(CollationCaseFirst caseFirst) {
    this.caseFirst = caseFirst;
  }

  public CollationStrength getStrength() {
    return strength;
  }

  public void setStrength(CollationStrength strength) {
    this.strength = strength;
  }

  public Boolean getNumericOrdering() {
    return numericOrdering;
  }

  public void setNumericOrdering(Boolean numericOrdering) {
    this.numericOrdering = numericOrdering;
  }

  public CollationAlternate getAlternate() {
    return alternate;
  }

  public void setAlternate(CollationAlternate alternate) {
    this.alternate = alternate;
  }

  public CollationMaxVariable getMaxVariable() {
    return maxVariable;
  }

  public void setMaxVariable(CollationMaxVariable maxVariable) {
    this.maxVariable = maxVariable;
  }

  public Boolean getNormalization() {
    return normalization;
  }

  public void setNormalization(Boolean normalization) {
    this.normalization = normalization;
  }

  public Boolean getBackwards() {
    return backwards;
  }

  public void setBackwards(Boolean backwards) {
    this.backwards = backwards;
  }

  public static final class Builder {
    private String locale;
    private Boolean caseLevel;
    private CollationCaseFirst caseFirst;
    private CollationStrength strength;
    private Boolean numericOrdering;
    private CollationAlternate alternate;
    private CollationMaxVariable maxVariable;
    private Boolean normalization;
    private Boolean backwards;

    private Builder() {
    }

    public Builder locale(String val) {
      locale = val;
      return this;
    }

    public Builder caseLevel(Boolean val) {
      caseLevel = val;
      return this;
    }

    public Builder caseFirst(CollationCaseFirst val) {
      caseFirst = val;
      return this;
    }

    public Builder strength(CollationStrength val) {
      strength = val;
      return this;
    }

    public Builder numericOrdering(Boolean val) {
      numericOrdering = val;
      return this;
    }

    public Builder alternate(CollationAlternate val) {
      alternate = val;
      return this;
    }

    public Builder maxVariable(CollationMaxVariable val) {
      maxVariable = val;
      return this;
    }

    public Builder normalization(Boolean val) {
      normalization = val;
      return this;
    }

    public Builder backwards(Boolean val) {
      backwards = val;
      return this;
    }

    public Collation build() {
      return new Collation(this);
    }
  }
}
