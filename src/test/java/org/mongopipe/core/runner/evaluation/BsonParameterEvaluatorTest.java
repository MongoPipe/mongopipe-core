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

package org.mongopipe.core.runner.evaluation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

public class BsonParameterEvaluatorTest {


  private static List<String> test(String line, Pattern pattern) {
    System.out.println("----------");
    System.out.println(line);
    System.out.println(pattern.toString());

    // Now create matcher object.
    Matcher matcher = pattern.matcher(line);
    System.out.println("Matches: " + matcher.matches());
    boolean found = false;
    List groups = new ArrayList();
    while (matcher.find()) {
      System.out.println("I found the text '" + matcher.group() + "' starting at " + matcher.start() + " ending at " + matcher.end());
      groups.add(matcher.group());
      found = true;
    }
    if (!found) {
      System.out.println("No match found.%n");
    }
    return groups;
  }

  @Test
  public void testRegex() {
    //    Pattern pattern = BsonParameterEvaluator.PARAMETER_PATTERN;
    //    Pattern pattern = Pattern.compile("(?<=\\$)\\w+");
    //    assertEquals(Arrays.asList("unu"), test("$unu", pattern));
    //    assertEquals(Arrays.asList("unu", "trei"), test("zero $unu \ndoi $trei", pattern));

    Pattern pattern = Pattern.compile("(?<=\\$\\{)[^\\}]+(?=\\})");
    assertEquals(Arrays.asList("unu"), test("${unu}", pattern));
    assertEquals(Arrays.asList("unu"), test("zero${unu}doi", pattern));
    assertEquals(Arrays.asList("unu", "trei"), test("zero ${unu} doi ${trei} patru", pattern));

    assertEquals(Arrays.asList("unu", "doi"), test("un\n\t ;-{}$ u${unu}do\ni${doi}trei", pattern));
    assertEquals(Arrays.asList("float(pizzaPrice)"), test("[{\"$match\": {\"price\": \"${float(pizzaPrice)}\"}}]", pattern));

    //    assertEquals(Arrays.asList("float", "pizzaPrice"),
    //        Arrays.asList("${float(pizzaPrice)}" .split(BsonParameterEvaluator.INSIDE_PATTERN)).stream()
    //            .filter((s) -> !s.trim().isEmpty())
    //            .collect(Collectors.toList()));
  }
}
