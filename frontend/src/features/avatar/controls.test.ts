import { describe, expect, it } from 'vitest';
import { applyRigControls, controlNames, emotionStates, expressionTarget, neutralControls, normalizeControls, type NumericRigInput } from './controls';
describe('semantic avatar controls',()=> {
  it('clamps values and preserves signed valence',()=> {
    const result=normalizeControls({anger:9,happiness:-2,valence:-.7,arousal:NaN,sarcasm:Infinity});
    expect(result).toMatchObject({anger:1,happiness:0,valence:-.7,arousal:.15,sarcasm:0});
    expect(normalizeControls({valence:-4}).valence).toBe(-1);
  });
  it('supports every required state with a safe neutral reset',()=> {
    expect(expressionTarget('neutral')).toEqual(neutralControls);
    for (const state of emotionStates) {
      const target=expressionTarget(state); expect(Object.keys(target)).toEqual([...controlNames]);
      for (const name of controlNames) {expect(Number.isFinite(target[name])).toBe(true);expect(target[name]).toBeLessThanOrEqual(1);expect(target[name]).toBeGreaterThanOrEqual(name==='valence' ? -1 : 0);}
    }
    expect(expressionTarget('angry').anger).toBe(.8);
    expect(expressionTarget('happy').happiness).toBe(.8);
    expect(expressionTarget('sad',NaN).sadness).toBe(.8);
  });
  it('writes all validated numeric inputs and reverses back to neutral',()=> {
    const inputs:NumericRigInput[]=controlNames.map(name=>({name,type:56,value:0}));
    applyRigControls(inputs,expressionTarget('angry'),56); expect(inputs.find(i=>i.name==='anger')?.value).toBe(80);
    expect(inputs.find(i=>i.name==='valence')?.value).toBe(-80);
    applyRigControls(inputs,neutralControls,56); expect(inputs.find(i=>i.name==='anger')?.value).toBe(0);
  });
  it('rejects missing, wrong-type and duplicate inputs before mutating a rig',()=> {
    const inputs:NumericRigInput[]=controlNames.map(name=>({name,type:56,value:.25}));
    expect(()=>applyRigControls(undefined,neutralControls,56)).toThrow('anger');
    expect(()=>applyRigControls(inputs.slice(0,-1),neutralControls,56)).toThrow('sarcasm');
    expect(inputs.every(i=>i.value===.25)).toBe(true);
    inputs.at(-1)!.type=59; expect(()=>applyRigControls(inputs,neutralControls,56)).toThrow('sarcasm');
    inputs.at(-1)!.type=56; expect(()=>applyRigControls([...inputs,inputs[0]],neutralControls,56)).toThrow('anger');
    expect(inputs.every(i=>i.value===.25)).toBe(true);
  });
});
