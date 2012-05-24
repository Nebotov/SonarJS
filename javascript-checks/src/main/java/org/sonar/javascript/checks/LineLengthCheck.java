/*
 * Sonar JavaScript Plugin
 * Copyright (C) 2011 Eriks Nukis and SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.javascript.checks;

import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.javascript.api.EcmaScriptGrammar;

@Rule(
  key = "LineLength",
  priority = Priority.MINOR)
public class LineLengthCheck extends SquidCheck<EcmaScriptGrammar> implements AstAndTokenVisitor {

  private static final int DEFAULT_MAXIMUM_LINE_LENHGTH = 80;

  @RuleProperty(
    key = "maximumLineLength",
    defaultValue = "" + DEFAULT_MAXIMUM_LINE_LENHGTH)
  public int maximumLineLength = DEFAULT_MAXIMUM_LINE_LENHGTH;

  public int getMaximumLineLength() {
    return maximumLineLength;
  }

  private int lastIncorrectLine;

  @Override
  public void visitFile(AstNode astNode) {
    lastIncorrectLine = -1;
  }

  public void visitToken(Token token) {
    int length = token.getColumn() + token.getValue().length();
    if (!token.isGeneratedCode() && lastIncorrectLine != token.getLine() && length > getMaximumLineLength()) {
      lastIncorrectLine = token.getLine();
      // Note that method from AbstractLineLengthCheck generates other message - see SONARPLUGINS-1809
      getContext().createLineViolation(this,
          "The line contains {0,number,integer} characters which is greater than {1,number,integer} authorized.",
          token.getLine(),
          length,
          getMaximumLineLength());
    }
  }

}
