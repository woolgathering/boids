# sundstrom-scExtensions
Extensions for SuperCollider

## Dependencies
Yes, dependencies. For Boids2D to work you need my fork of [VectorSpace](https://github.com/woolgathering/VectorSpace).

## Included
### Classes
- Automator
- Boids2D
- WriteFFT
- SinglePole

### Class Extensions
- Array
  - noiseInterpolate()
- ArrayedCollection
  - interpolate()
- Buffer
  - \*makeKernel() and makeKernel()
- Date
  - yesterday(), tomorrow(), getDayOfWeek()
- String
  - stripNewlines()

### Pseudo-UGens
Some of these need to be reworked...
- AutoBFormat_fromStereo
- NanFilter (superceeded by Sanitize)
- NoiseVol
- RunningRange
- TimeStretch
- Distance
