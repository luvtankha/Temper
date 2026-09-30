import { afterEach, describe, expect, it } from 'vitest';
import { render, screen, cleanup } from '@testing-library/react';
import { RemoteAvatar } from './RemoteAvatar';
afterEach(cleanup);
describe('remote participant identity',()=> {
  it('shows Nova for Alex',()=> { render(<RemoteAvatar localId="alex" />); expect(screen.getByAltText('Nova, neutral female avatar preview')).toHaveAttribute('src','/avatars/female.svg'); expect(screen.getByLabelText("Nova's remote avatar")).toHaveAttribute('data-participant-id','nova'); });
  it('shows Alex for Nova and updates after participant switch',()=> { const {rerender}=render(<RemoteAvatar localId="alex" />); rerender(<RemoteAvatar localId="nova" />); expect(screen.getByAltText('Alex, neutral male avatar preview')).toHaveAttribute('src','/avatars/male.svg'); expect(screen.queryByLabelText("Nova's remote avatar")).not.toBeInTheDocument(); });
});
