export const rigConfig = {
  male: {src:import.meta.env.VITE_MALE_RIG_URL || `${import.meta.env.BASE_URL}avatars/male.riv`, artboard:'TemperMale'},
  female: {src:import.meta.env.VITE_FEMALE_RIG_URL || `${import.meta.env.BASE_URL}avatars/female.riv`, artboard:'TemperFemale'},
};
export const rigStateMachine='TemperEmotion';
