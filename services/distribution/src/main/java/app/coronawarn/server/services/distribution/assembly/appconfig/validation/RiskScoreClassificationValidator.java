/*
 * Corona-Warn-App
 *
 * SAP SE and all other contributors /
 * copyright owners license this file to you under the Apache
 * License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package app.coronawarn.server.services.distribution.assembly.appconfig.validation;

import static app.coronawarn.server.services.distribution.assembly.appconfig.validation.RiskScoreClassificationValidationError.ErrorType.BLANK_LABEL;
import static app.coronawarn.server.services.distribution.assembly.appconfig.validation.RiskScoreClassificationValidationError.ErrorType.INVALID_PARTITIONING;
import static app.coronawarn.server.services.distribution.assembly.appconfig.validation.RiskScoreClassificationValidationError.ErrorType.INVALID_URL;
import static app.coronawarn.server.services.distribution.assembly.appconfig.validation.RiskScoreClassificationValidationError.ErrorType.MIN_GREATER_THAN_MAX;
import static app.coronawarn.server.services.distribution.assembly.appconfig.validation.RiskScoreClassificationValidationError.ErrorType.VALUE_OUT_OF_BOUNDS;

import app.coronawarn.server.common.protocols.internal.RiskScoreClass;
import app.coronawarn.server.common.protocols.internal.RiskScoreClassification;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * The RiskScoreClassificationValidator validates the values of an associated {@link RiskScoreClassification} instance.
 */
public class RiskScoreClassificationValidator extends AppConfigurationValidator {

  /**
   * This defines the number of possible values (0 ... RISK_SCORE_VALUE_RANGE - 1) for the total risk score.
   */
  public static final int RISK_SCORE_VALUE_RANGE = 256;

  private final RiskScoreClassification riskScoreClassification;

  public RiskScoreClassificationValidator(RiskScoreClassification riskScoreClassification) {
    this.riskScoreClassification = riskScoreClassification;
  }

  /**
   * Performs a validation of the associated {@link RiskScoreClassification} instance and returns information about
   * validation failures.
   *
   * @return The ValidationResult instance, containing information about possible errors.
   */
  @Override
  public ValidationResult validate() {
    errors = new ValidationResult();

    validateValues();
    validateValueRangeCoverage();

    return errors;
  }

  private void validateValues() {
    for (RiskScoreClass riskScoreClass : riskScoreClassification.getRiskScoreClassesList()) {
      int minRiskLevel = riskScoreClass.getMinRiskLevel();
      int maxRiskLevel = riskScoreClass.getMaxRiskLevel();

      validateLabel(riskScoreClass.getLabel());
      validateRiskScoreValueBounds(minRiskLevel);
      validateRiskScoreValueBounds(maxRiskLevel);
      validateUrl(riskScoreClass.getUrl());

      if (minRiskLevel > maxRiskLevel) {
        errors.add(new RiskScoreClassificationValidationError(
            "minRiskLevel, maxRiskLevel", minRiskLevel + ", " + maxRiskLevel, MIN_GREATER_THAN_MAX));
      }
    }
  }

  private void validateLabel(String label) {
    if (label.isBlank()) {
      errors.add(new RiskScoreClassificationValidationError("label", label, BLANK_LABEL));
    }
  }

  private void validateRiskScoreValueBounds(int value) {
    if (value < 0 || value > RISK_SCORE_VALUE_RANGE - 1) {
      errors.add(new RiskScoreClassificationValidationError("minRiskLevel/maxRiskLevel", value, VALUE_OUT_OF_BOUNDS));
    }
  }

  private void validateUrl(String url) {
    if (!url.isBlank()) {
      try {
        new URL(url);
      } catch (MalformedURLException e) {
        errors.add(new RiskScoreClassificationValidationError("url", url, INVALID_URL));
      }
    }
  }

  private void validateValueRangeCoverage() {
    int partitionSum = riskScoreClassification.getRiskScoreClassesList().stream()
        .mapToInt(riskScoreClass -> (riskScoreClass.getMaxRiskLevel() - riskScoreClass.getMinRiskLevel() + 1))
        .sum();

    if (partitionSum != RISK_SCORE_VALUE_RANGE) {
      errors.add(new RiskScoreClassificationValidationError("covered value range", partitionSum, INVALID_PARTITIONING));
    }
  }
}
