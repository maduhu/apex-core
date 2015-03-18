/**
 * Copyright (c) 2012-2013 DataTorrent, Inc.
 * All rights reserved.
 */
package com.datatorrent.stram.webapp.asm;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.objectweb.asm.signature.SignatureVisitor;

import com.datatorrent.stram.webapp.asm.Type.ArrayTypeNode;
import com.datatorrent.stram.webapp.asm.Type.ParameterizedTypeNode;
import com.datatorrent.stram.webapp.asm.Type.TypeNode;
import com.datatorrent.stram.webapp.asm.Type.TypeVariableNode;
import com.datatorrent.stram.webapp.asm.Type.WildcardTypeNode;

/**
 * Follow the visiting path of ASM
 * to visit getter and setter method signature
 * 
 * ClassSignature = ( visitFormalTypeParameter visitClassBound? visitInterfaceBound* )* ( visitSuperClass visitInterface* )
 * MethodSignature = ( visitFormalTypeParameter visitClassBound? visitInterfaceBound* )* ( visitParameterType* visitReturnType visitExceptionType* )
 * TypeSignature = visitBaseType | visitTypeVariable | visitArrayType | ( visitClassType visitTypeArgument* ( visitInnerClassType visitTypeArgument* )* visitEnd ) )
 * 
 */
public abstract class BaseSignatureVisitor implements SignatureVisitor
{
  
  protected List<TypeVariableNode> typeV = new LinkedList<Type.TypeVariableNode>();
  
  protected int stage = -1;
  
  public static final int VISIT_FORMAL_TYPE = 0;
  
  protected Stack<Type> visitingStack = new Stack<Type>();
  
  protected String signature = "";


  
  @Override
  public SignatureVisitor visitArrayType()
  {
    //System.out.println("visitArrayType　");
    ArrayTypeNode at = new ArrayTypeNode();
    visitingStack.push(at);
    return this;
  }

  @Override
  public void visitBaseType(char baseType)
  {
    
    Type.TypeNode tn = new Type.TypeNode();
    tn.typeObj = org.objectweb.asm.Type.getType(baseType + "");
//    System.out.println(tn.typeObj);
    visitingStack.push(tn);
    resolveStack();
    // base type could only appear in method parameter list or return type  
//    if(stage == VISIT_PARAM) {
//      visitingStack.push(tn);
//    }
//    if(stage == VISIT_RETURN) {
//      returnType = tn;
//    }
    //System.out.println("visitBaseType:'" + baseType);
  }


  @Override
  public void visitClassType(String classType)
  { 

    Type.TypeNode tn = new Type.TypeNode();
    tn.typeObj = org.objectweb.asm.Type.getType("L" + classType + ";");
//    System.out.println(tn.typeObj);
    visitingStack.push(tn);
    // base type could only appear in method parameter list or return type  
//    if(stage == VISIT_PARAM) {
//      visitingStack.push(tn);
//    }
//    if(stage == VISIT_RETURN) {
//      returnType = tn;
//    } if(stage == VISIT_EXCEPTION) {
//      exceptionType = tn;
//    } 
    //System.out.print("visitClassType:'" + classType + "'  ,  ");
  }
  
  private void resolveStack() {
    if(visitingStack.isEmpty() || visitingStack.size()==1){
      return;
    }
    Type top = visitingStack.pop();
    Type peek = visitingStack.peek();
    
    if(peek instanceof ParameterizedTypeNode){
      ((ParameterizedTypeNode)peek).actualTypeArguments.add(top);
      return;
    } else if(peek instanceof ArrayTypeNode) {
      ((ArrayTypeNode)peek).actualArrayType = top;
      resolveStack();
    } else if(peek instanceof WildcardTypeNode) {
      ((WildcardTypeNode)peek).bounds.add(top);
      resolveStack();
    } else if(peek instanceof TypeVariableNode){
      ((TypeVariableNode)peek).bounds.add(top);
      resolveStack();
    } else {
      visitingStack.push(top);
      return;
    }
    
  }

  @Override
  public void visitEnd()
  {
    resolveStack();
    //System.out.print("visitEnd　");
    //System.out.println();
  }
  
  @Override
  public void visitInnerClassType(String classType)
  {
    visitClassType(classType); 
    //System.out.print("visitInnerClassType:'" + classType + "'  ,  ");
  }


  @Override
  public void visitTypeArgument()
  { 

    
  }

  @Override
  public SignatureVisitor visitTypeArgument(char typeArg)
  {
    TypeNode t = (TypeNode) visitingStack.pop();
    if (t instanceof ParameterizedTypeNode) {
      visitingStack.push(t);
    } else {
      ParameterizedTypeNode pt = new ParameterizedTypeNode();
      pt.typeObj = t.typeObj;
      visitingStack.push(pt);
    }
    
    if(typeArg == SignatureVisitor.INSTANCEOF){
      return this;
    }        
    WildcardTypeNode wtn = new WildcardTypeNode();
    wtn.boundChar = typeArg;
    visitingStack.push(wtn);
    
    //System.out.print("visitTypeArgument:'" + typeArg + "'  ,  ");
    return this;
  }
  
  

  @Override
  public void visitTypeVariable(String typeVariable)
  {
    boolean found = false;
    for (TypeVariableNode typeVariableNode : typeV) {
      if(typeVariableNode.typeLiteral.equals(typeVariable)){
        visitingStack.push(typeVariableNode);
        found = true;
        break;
      }
    }
    if(!found) {
      TypeNode tn = new TypeNode();
      tn.typeObj = org.objectweb.asm.Type.getType("T" + typeVariable + ";");
      visitingStack.push(tn);
      
    }
    resolveStack();
    //System.out.println("visitTypeVariable:'" + typeVariable);
  }
  
  @Override
  public SignatureVisitor visitInterface()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public SignatureVisitor visitInterfaceBound()
  {
//    System.out.println("*******************visitInterfaceBound");
//    System.out.println("visitInterfaceBound");
    return this;
  }

  @Override
  public SignatureVisitor visitSuperclass()
  {
    
    throw new UnsupportedOperationException();
  }
  
  @Override
  public void visitFormalTypeParameter(String typeVariable)
  {
    if(stage==VISIT_FORMAL_TYPE && !visitingStack.isEmpty()){
      visitingStack.pop();
    }
    stage = VISIT_FORMAL_TYPE;
    TypeVariableNode tvn = new TypeVariableNode();
    tvn.typeLiteral = typeVariable;
    visitingStack.push(tvn);
    typeV.add(tvn);
//    System.out.println("****************" + typeVariable);
//    System.out.println("visitFormalTypeParameter");

//    throw new UnsupportedOperationException();
  }

  @Override
  public SignatureVisitor visitClassBound()
  {
//    System.out.println("*******************visitClassBound");
    return this;
    //throw new UnsupportedOperationException();
  }

  
}