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

import org.bson.*;
import org.bson.conversions.Bson;
import org.mongopipe.core.exception.MongoPipeConfigException;
import org.mongopipe.core.exception.MongoPipeRunException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mongopipe.core.util.BsonUtil.toBsonValue;

/**
 * Internal class.
 * Having native BSON means that expressions like <code>"price": $price</code> are not valid bson because <code>$price</code> although is
 * intended to be a number should be placed into double quotes and thus will prevent the usage of template engines because the double quotes
 * will remain after evaluation.
 * <p>
 * Still native BSON is preferred over BSON represented as String because:
 * 1. It is Mongo format, it is natural to work with Documents, to navigate than a compacted String, from tools like MongoCompass.
 * 2. The pipeline commands expect a bson/List<bson> as the pipeline, thus conversion to Bson was anyway necessary.
 * 3. eliminates the need to convert to string, apply a string template engine and then back to bson and:
 * TODO: opens the door for BSON paths (e.g. pipeline[0]/$match/size) detection on pipeline creation time and creation of generic
 * setters for each path. This would more efficient as the parameters are directly addressed without the need of discovery each time.
 * <p>
 * This class evaluates the parameters from a BsonDocument or List<BsonDocument> and replaces them with user provided values.
 * The parent is the one that decides the placement of the child evaluated value via passed in consumer. Might be changed in future.
 */
public class BsonParameterEvaluator {
  // Could not use '${param}' because JsonScanner considers the { as a start of a new document. Would work only if enclosing value in quotes
  // which may not seem natural for the user when the value let's say is a number, boolean, or document/map, so stay with $ for the moment.
  public static final String DOLLAR = "$";
  //public static final Pattern PARAMETER_PATTERN = Pattern.compile("(?<=\\$\\{)[^\\}]+(?=\\})"); //  "(?<=\\$)\\w+" for $, restrictive.
  public static final Pattern PARAMETER_PATTERN = Pattern.compile("(?<=\\$)\\w+");
  //public static final String INSIDE_PATTERN = "[${()}]"; // Splits "${float(pizzaPrice)}" in ["float", "pizzaPrice"].

  private Map<String, Object> parameters;

  interface ParamFunction {
    BsonValue apply(String functionParam, Map<String, Object> pipelineParams);
  }

  private static Map<String, ParamFunction> FUNCTIONS = new HashMap<>();

  static {
    FUNCTIONS.put("float", (functionParam, paramsMap) -> {
      if (!paramsMap.containsKey(functionParam)) {
        throw new MongoPipeConfigException("Missing parameter value for " + functionParam);
      }
      Object value = paramsMap.get(functionParam);
      try {
        return new BsonDouble(Double.parseDouble(value.toString()));
      } catch (NumberFormatException numberFormatException) {
        throw new MongoPipeRunException("Double/float parameter expected, got instead:" + value, numberFormatException);
      }
    });
  }

  public BsonParameterEvaluator(Map<String, ?> parameters) {
    this.parameters = (Map<String, Object>) parameters;
  }

  private class Match {
    String value;
    int start;
    int end;

    public Match(String value, int start, int end) {
      this.value = value;
      this.start = start;
      this.end = end;
    }
  }

  private List<Match> match(String value) {
    Matcher matcher = PARAMETER_PATTERN.matcher(value);
    List groups = new ArrayList();
    while (matcher.find()) {
      //System.out.println("I found the text '" + matcher.group() + "' starting at " + matcher.start() + " ending at " + matcher.end());
      groups.add(new Match(matcher.group(), matcher.start(), matcher.end()));
    }
    return groups;
  }

  private void navigate(BsonDocument parentBsonDocument, BsonValue bsonValue, Consumer<BsonValue> function) {
    if (bsonValue == null) {
      return;
    }
    if (bsonValue.isArray()) {  // TODO: array test, where one element is the parameter or the entire array list, might need veryfying the type of the actual param value and converting to a BsonArray or BsonDocument.
      BsonArray bsonArray = (BsonArray) bsonValue;
      for (final AtomicInteger i = new AtomicInteger(0); i.get() < bsonArray.size(); i.incrementAndGet()) {
        // The parent provides the context function and the child will provide the value.
        navigate(parentBsonDocument, bsonValue, newBsonValue -> bsonArray.set(i.get(), newBsonValue));
      }
    } else if (bsonValue.isDocument()) {
      BsonDocument bsonDocument = ((BsonDocument) bsonValue);
      // The parent provides the context function and the child will provide the value.
      bsonDocument.forEach((key, bsonValue1) ->
          navigate(bsonDocument, bsonValue1, newBsonValue -> bsonDocument.put(key, newBsonValue)));

    } else if (bsonValue.isString()) {   // Interested in the String values as these will keep the parameters that are String or non String.
      String bsonStringValue = ((BsonString) bsonValue).getValue();

      AtomicInteger current = new AtomicInteger(0);
      AtomicInteger end = new AtomicInteger(bsonStringValue.length());
      StringBuilder newStringValue = new StringBuilder();
      AtomicReference<BsonValue> newValue = new AtomicReference<>();
      if (bsonStringValue.contains(DOLLAR)) {
        List<Match> matches = match(bsonStringValue);
        // Replace potential parameters.
        if (matches.size() == 1 && parameters.containsKey(matches.get(0).value)) {
           Match match = matches.get(0);
           Object matchValue = match.value;
           Object actualValue = parameters.getOrDefault(matchValue, matchValue);
           if (!(actualValue instanceof String)) {
             function.accept(toBsonValue(actualValue));
           } else {
             // Add anything that is around.
             newStringValue.append(bsonStringValue.substring(current.get(), match.start - 1));
             newStringValue.append(parameters.getOrDefault(matchValue, matchValue));
             newStringValue.append(bsonStringValue.substring(match.end));
             function.accept(toBsonValue(newStringValue.toString()));
           }
        }
      }
    }
  }


  public void evaluate(Bson bson) {
    BsonDocument bsonDocument = bson.toBsonDocument();
    bsonDocument.forEach((key, bsonValue) -> {
      // The parent provides the context function and the child provides the replacing value.
      navigate(bsonDocument, bsonValue, newBsonValue -> bsonDocument.put(key, newBsonValue));
    });
  }

  public List<BsonDocument> evaluate(List<BsonDocument> bsonDocumentList) {
    bsonDocumentList.forEach(bsonDocument -> evaluate(bsonDocument));
    return bsonDocumentList;
  }

//  /**
//   * Evaluate Document or BsonDocument and replace parameters with actual values.
//   */
//  public Document evaluate(Bson document) {
//    String documentString = document.toString();
//
////    // 1. Configure FreeMarker
////    // You should do this ONLY ONCE, when your application starts,
////    // then reuse the same Configuration object elsewhere.
////    Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
////
////    cfg.setDefaultEncoding("UTF-16");
////    try {
////      Template template = new Template("name", new StringReader(documentString), cfg);
////      StringWriter out = new StringWriter();
////      template.process(parameters, out);
////      out.flush();
////      documentString = out.toString();
////    } catch (IOException | TemplateException e) {
////      throw new InvalidPipelineTemplateException("Invalid template", e);
////    }
//    return Document.parse(documentString);
//  }


}
