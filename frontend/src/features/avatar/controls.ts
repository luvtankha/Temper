export const emotionStates = ['neutral','happy','concerned','confused','sad','frustrated','angry','surprised'] as const;
export type EmotionState = typeof emotionStates[number];
export const controlNames = ['anger','sadness','happiness','frustration','confusion','concern','surprise','valence','arousal','sarcasm'] as const;
export type ControlName = typeof controlNames[number];
export type AvatarControls = Record<ControlName,number>;
export const neutralControls: AvatarControls = {anger:0,sadness:0,happiness:0,frustration:0,confusion:0,concern:0,surprise:0,valence:0,arousal:.15,sarcasm:0};

export function normalizeControls(input:Partial<AvatarControls>): AvatarControls {
  return Object.fromEntries(controlNames.map(name => {
    const value=input[name];
    const safe = typeof value === 'number' && Number.isFinite(value) ? value : neutralControls[name];
    return [name, Math.min(1,Math.max(name === 'valence' ? -1 : 0,safe))];
  })) as AvatarControls;
}

/** Engineering test targets for rig authoring, not classifier predictions. */
export function expressionTarget(state:EmotionState,intensity=.8): AvatarControls {
  const amount=Number.isFinite(intensity) ? Math.max(0,Math.min(1,intensity)) : .8;
  const target={...neutralControls};
  const key: Partial<Record<EmotionState,ControlName>> = {happy:'happiness',concerned:'concern',confused:'confusion',sad:'sadness',frustrated:'frustration',angry:'anger',surprised:'surprise'};
  const selected=key[state]; if (selected) target[selected]=amount;
  target.valence=state==='happy' ? amount : ['sad','frustrated','angry','concerned'].includes(state) ? -amount : 0;
  target.arousal=state==='neutral' ? .15 : state==='sad' ? .2*amount : ['angry','frustrated','surprised'].includes(state) ? amount : .5*amount;
  return normalizeControls(target);
}

export interface NumericRigInput { name:string; type:number; value:number|boolean; }
/** Validate the entire rig before mutating any input. Wrong rigs must not half-update. */
export function applyRigControls(inputs:NumericRigInput[]|undefined, controls:AvatarControls, numberType:number): void {
  const required=controlNames.map(name => {
    const matches=inputs?.filter(input=>input.name===name) ?? [];
    if (matches.length!==1 || matches[0].type!==numberType) throw new Error(`Rig requires one numeric input: ${name}`);
    return matches[0];
  });
  const safe=normalizeControls(controls);
  required.forEach(input=> {input.value=safe[input.name as ControlName];});
}
