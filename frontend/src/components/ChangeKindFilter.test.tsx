import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ALL_CHANGE_KINDS } from '../analysis/ChangeKind'
import { ChangeKindFilter } from './ChangeKindFilter'

describe('ChangeKindFilter',()=>{
  it('defaults to All when all categories are selected',()=>{render(<ChangeKindFilter value={[...ALL_CHANGE_KINDS]} onChange={()=>undefined}/>);expect(screen.getByLabelText('Change type')).toHaveValue('all')})
  it('selects the code preset',()=>{const onChange=vi.fn();render(<ChangeKindFilter value={[...ALL_CHANGE_KINDS]} onChange={onChange}/>);fireEvent.change(screen.getByLabelText('Change type'),{target:{value:'code'}});expect(onChange).toHaveBeenCalledWith(['CODE'])})
  it('supports custom CI/CD and Other selections',()=>{const onChange=vi.fn();render(<ChangeKindFilter value={['CI_CD']} onChange={onChange}/>);fireEvent.change(screen.getByLabelText('Change type'),{target:{value:'custom'}});expect(screen.getByText('CI/CD')).toBeInTheDocument();expect(screen.getByText('Other')).toBeInTheDocument()})
})
